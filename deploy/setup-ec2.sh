#!/bin/bash
# ============================================
# EC2 Initial Setup Script for FinEasy
# Run on a fresh Amazon Linux 2023 / Ubuntu EC2
# Usage: chmod +x setup-ec2.sh && sudo ./setup-ec2.sh
# ============================================

set -e

echo "=========================================="
echo "  FinEasy EC2 Setup"
echo "=========================================="

# Detect OS
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
fi

# ---- 1. Swap Memory (essential for t2.micro) ----
echo "[1/5] Setting up 2GB swap..."
if [ ! -f /swapfile ]; then
    fallocate -l 2G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
    echo "Swap created successfully"
else
    echo "Swap already exists, skipping"
fi

# ---- 2. Install Docker ----
echo "[2/5] Installing Docker..."
if ! command -v docker &> /dev/null; then
    if [ "$OS" = "amzn" ]; then
        yum update -y
        yum install -y docker
        systemctl start docker
        systemctl enable docker
    else
        apt-get update
        apt-get install -y ca-certificates curl gnupg
        install -m 0755 -d /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
        chmod a+r /etc/apt/keyrings/docker.gpg
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
        apt-get update
        apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    fi
    usermod -aG docker ec2-user 2>/dev/null || usermod -aG docker ubuntu 2>/dev/null || true
    echo "Docker installed"
else
    echo "Docker already installed, skipping"
fi

# ---- 3. Install Docker Compose (if not bundled) ----
echo "[3/5] Checking Docker Compose..."
if ! docker compose version &> /dev/null; then
    COMPOSE_VERSION="v2.24.5"
    curl -SL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    echo "Docker Compose installed"
else
    echo "Docker Compose already available"
fi

# ---- 4. Create project directory ----
echo "[4/5] Creating project directory..."
PROJECT_DIR="/home/$(logname 2>/dev/null || echo ec2-user)/fineasy"
mkdir -p "$PROJECT_DIR/deploy"
chown -R "$(logname 2>/dev/null || echo ec2-user):$(logname 2>/dev/null || echo ec2-user)" "$PROJECT_DIR"
echo "Project directory: $PROJECT_DIR"

# ---- 5. Firewall / Security reminder ----
echo "[5/5] Setup complete!"
echo ""
echo "=========================================="
echo "  NEXT STEPS"
echo "=========================================="
echo ""
echo "1. AWS Security Group: Open ports 80 (HTTP), 22 (SSH)"
echo ""
echo "2. Clone your repo:"
echo "   cd ~/fineasy"
echo "   git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git ."
echo ""
echo "3. Create .env file:"
echo "   cp .env.example .env"
echo "   nano .env    # Fill in your secrets"
echo ""
echo "4. Login to GitHub Container Registry:"
echo "   echo YOUR_GITHUB_TOKEN | docker login ghcr.io -u YOUR_USERNAME --password-stdin"
echo ""
echo "5. Start the application:"
echo "   docker compose -f docker-compose.prod.yml up -d"
echo ""
echo "6. Check status:"
echo "   docker compose -f docker-compose.prod.yml ps"
echo "   docker compose -f docker-compose.prod.yml logs -f"
echo ""
echo "=========================================="
