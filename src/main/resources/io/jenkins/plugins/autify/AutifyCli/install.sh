URL="https://autify-cli-assets.s3.amazonaws.com/autify-cli/channels/stable/install-cicd.bash"
if [ $(command -v curl) ]; then
  curl "$URL" | bash -xe
else
  wget -O- "$URL" | bash -xe
fi
