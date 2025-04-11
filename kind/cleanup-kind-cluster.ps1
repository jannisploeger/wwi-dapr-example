# Cleanup script for KIND cluster, Dapr, and Kubernetes context

# Define the cluster name
$clusterName = "dapr-cluster"

# Define the kubeconfig context name
$contextName = "kind-$clusterName"

# Remove the Dapr installation from Kubernetes
Write-Host "Removing Dapr from the Kubernetes cluster." -ForegroundColor Yellow
dapr uninstall --kubernetes
Write-Host "Dapr uninstalled from the Kubernetes cluster." -ForegroundColor Green

# Delete the KIND cluster
Write-Host "Deleting the KIND cluster '$clusterName'." -ForegroundColor Yellow
kind delete cluster --name $clusterName
Write-Host "KIND cluster '$clusterName' deleted." -ForegroundColor Green

# Remove the kubeconfig context
Write-Host "Cleaning up the kubeconfig context '$contextName'." -ForegroundColor Yellow
kubectl config delete-context $contextName | Out-Null
Write-Host "Kubeconfig context '$contextName' removed." -ForegroundColor Green

# Optionally, delete the related Kubernetes namespace for Dapr (if it wasn't automatically removed)
$daprNamespace = "dapr-system"
Write-Host "Checking for the Dapr namespace '$daprNamespace'." -ForegroundColor Yellow
if (kubectl get namespace $daprNamespace -o name | Out-Null) {
    kubectl delete namespace $daprNamespace
    Write-Host "Namespace '$daprNamespace' deleted." -ForegroundColor Green
} else {
    Write-Host "Namespace '$daprNamespace' not found, skipping deletion." -ForegroundColor Cyan
}