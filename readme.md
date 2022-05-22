# Gitlab Tags

Quick and dirty scala script to get the newest images from gitlab container registry

GET /projects/:id/registry/repositories
GET /projects/:id/registry/repositories/:repository_id/tags
GET /projects/:id/registry/repositories/:repository_id/tags/:tag_name

# Example


```bash
./gitlab-tags.sc --projectId 1234567890
```

