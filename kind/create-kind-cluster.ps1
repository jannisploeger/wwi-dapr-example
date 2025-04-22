# Create a KIND cluster, set the kubeconfig context, initialize Dapr, build and load Docker images, and setup the Dapr dashboard

# Set the path to the KIND cluster configuration file
$configFile = "kind-cluster-config.yaml"

# Define the cluster name
$clusterName = "dapr-cluster"

# Define the kubeconfig context name
$contextName = "kind-$clusterName"

$imageNames = @(
    @{imageName = "shop:0.0.1-SNAPSHOT" }
    @{imageName = "billing:0.0.1-SNAPSHOT" }
    @{imageName = "warehouse:0.0.1-SNAPSHOT" }
)



# Check if the configuration file exists
if (Test-Path $configFile)
{
    Write-Host "Configuration file found: $configFile" -ForegroundColor Green

    # Create the cluster using the configuration file and set the cluster name
    kind create cluster --name $clusterName --config $configFile
    Write-Host "KIND cluster '$clusterName' creation initiated." -ForegroundColor Green

    # Set Kubernetes context to the new cluster
    Write-Host "Setting the kubeconfig context to '$contextName'." -ForegroundColor Yellow
    kubectl config use-context $contextName
    Write-Host "Kubeconfig context set to '$contextName'." -ForegroundColor Green

    # Initialize Dapr in Kubernetes
    # ---------------------------------------------------------------
    # The "dapr init --kubernetes" command installs Dapr components
    # into the Kubernetes cluster. These components include:
    # 1. Sidecar Injector - Injects the Dapr sidecar into pods.
    # 2. Operator - Manages the lifecycle of Dapr components.
    # 3. Placement Service - Supports actors in distributed systems.
    # 4. Dashboard - Provides a web-based UI for managing Dapr.
    # This sets up the Dapr runtime for building distributed applications.
    # ---------------------------------------------------------------
    Write-Host "Initializing Dapr in the Kubernetes cluster." -ForegroundColor Yellow
    dapr init --kubernetes
    Write-Host "Dapr initialization completed." -ForegroundColor Green

    # Wait until all Dapr components are running
    Write-Host "Waiting for all Dapr components to reach 'Running' status..." -ForegroundColor Yellow
    do
    {
        # Get the Dapr status and filter for components not in 'Running' state
        $statusOutput = dapr status -k
        $pendingComponents = $statusOutput | Select-String -Pattern "Pending|NotReady|Waiting"

        # Check if there are any pending or not ready components
        if ($pendingComponents)
        {
            Write-Host "Pending components detected. Waiting..." -ForegroundColor Yellow
            Start-Sleep -Seconds 10
        }
    } while ($pendingComponents)

    Write-Host "All Dapr components are running." -ForegroundColor Green


    # Build and load Docker images for all components

    # Load the Docker image into the KIND cluster
    foreach ($image in $imageNames) {
        $imageName = $image.imageName
        Write-Host "Loading Docker image '$imageName' into the KIND cluster '$clusterName'." -ForegroundColor Yellow

        $result = kind load docker-image $imageName --name $clusterName 2>&1
        $success = $LASTEXITCODE -eq 0

        if ($success) {
            Write-Host "Docker image '$imageName' loaded into the KIND cluster '$clusterName'." -ForegroundColor Green
        } else {
            Write-Host "Failed to load Docker image '$imageName'. Error: $result" -ForegroundColor Red
        }
    }
    # Start the Dapr dashboard on port 9999
    Write-Host "Starting the Dapr dashboard on port 9999." -ForegroundColor Yellow
    dapr dashboard -k -p 9999
}
else
{
    Write-Host "Configuration file not found: $configFile" -ForegroundColor Red
    exit 1
}