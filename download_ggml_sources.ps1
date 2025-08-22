# PowerShell script to download missing GGML source files from llama.cpp repository
# Run this script from the LocalAIIndia directory

$repoUrl = "https://raw.githubusercontent.com/ggml-org/llama.cpp/master"
$srcDir = "app/src/main/cpp/src"

# Create src directory if it doesn't exist
if (!(Test-Path $srcDir)) {
    New-Item -ItemType Directory -Path $srcDir -Force
}

# List of essential GGML source files needed
$ggmlFiles = @(
    "ggml.c",
    "ggml-alloc.c", 
    "ggml-backend.c",
    "ggml-cpu.c",
    "ggml-opt.c"
)

Write-Host "Downloading GGML source files from llama.cpp repository..."

foreach ($file in $ggmlFiles) {
    $url = "$repoUrl/$file"
    $destination = "$srcDir/$file"
    
    Write-Host "Downloading $file..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $destination
        Write-Host "✓ Downloaded $file"
    }
    catch {
        Write-Host "✗ Failed to download $file: $($_.Exception.Message)"
    }
}

Write-Host "`nDownload complete! Now update your CMakeLists.txt to include these files."
