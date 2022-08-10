URL="https://autify-cli-assets.s3.amazonaws.com/autify-cli/channels/stable/install-cicd.bash"
if [ $(command -v curl) ]; then
  curl "$URL" | bash
else
  wget -O- "$URL" | bash
fi
