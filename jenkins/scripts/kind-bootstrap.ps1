kind create cluster --wait 60s

kubectl apply --server-side=true `
  -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.5.1/standard-install.yaml

helm upgrade --install ngf `
  oci://ghcr.io/nginx/charts/nginx-gateway-fabric `
  --version 2.6.6 `
  --namespace nginx-gateway `
  --create-namespace `
  --wait