#!/usr/bin/env amm

import $ivy.`io.circe::circe-core:0.14.1`
import $ivy.`io.circe::circe-generic:0.14.1`
import $ivy.`io.circe::circe-parser:0.14.1`
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import mainargs.{main, arg, ParserForMethods, Flag, TokensReader}
import ammonite._

def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

@main
def run(
  @arg(doc = "projectId from the Repository") projectId: Int,
  ) = {

    val maybeRepoIds = getRepositories(projectId)

    // We can use string sort since the datetime is in iso format
    val res = maybeRepoIds.map { repos =>
      repos.flatMap { repo => 
        getTags(projectId, repo).toOption.map(tags => tags.flatMap(tag => getTagDetail(projectId, repo, tag).toOption).sortBy(_.created_at).reverse)
      }.flatten.take(3)
    }

    println(res)
}

case class Repository(id: Int)
case class Tag(name: String)
case class TagDetail(name: String, created_at: String)

def getRepositories(
    projectId: Int
): Either[Error, List[Repository]] = {
  val response = requests.get(
    s"https://gitlab.com/api/v4/projects/${projectId}/registry/repositories",
  )
  decode[List[Repository]](response.text)
}

def getTags(
    projectId: Int,
    repo: Repository
): Either[Error, List[Tag]] = {
  def getTagsPerPage(url: String): Either[Error, List[Tag]] = {
    val response = requests.get(url)
    val nextLink = response.headers("link").flatMap(_.split(",").find(_.contains("next")).map(_.split(";").head.trim.replace("<","").replace(">","")))
    val res: Either[Error, List[Tag]] = decode[List[Tag]](response.text)

    if(nextLink.isEmpty) res
    else for {
      first <- res
      next <- getTagsPerPage(nextLink.head)
    } yield first ++ next
  }

  val startUrl = s"https://gitlab.com/api/v4/projects/${projectId}/registry/repositories/${repo.id}/tags?per_page=100&page=1"

  getTagsPerPage(startUrl)
}

def getTagDetail(
    projectId: Int,
    repo: Repository,
    tag: Tag
): Either[Error, TagDetail] = {
  val response = requests.get(
    s"https://gitlab.com/api/v4/projects/${projectId}/registry/repositories/${repo.id}/tags/${tag.name}",
  )
  decode[TagDetail](response.text)
}
