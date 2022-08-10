mkdir -p "$NVM_DIR"

NVM_URL="https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh"
if [ $(command -v curl) ]; then
  curl "$NVM_URL" | bash
else
  wget -O- "$NVM_URL" | bash
fi

set +x
. "$NVM_DIR/nvm.sh"
nvm install --lts
nvm use --lts
node -v

set -x
npm install @autifyhq/autify-cli-integration-test
SCRIPT_DIR=$(dirname "$0")
mkdir -p ./bin
cp "$SCRIPT_DIR/autify-with-proxy" ./bin/autify-with-proxy
chmod +x ./bin/autify-with-proxy
