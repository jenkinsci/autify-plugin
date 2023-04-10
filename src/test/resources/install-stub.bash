#!/bin/bash
DIR=$PWD
mkdir -p "$DIR/autify/bin"

cat << EOS > "$DIR/autify/bin/autify"
#!/bin/bash
if [[ "\$@" == "--version" ]]; then
  echo "@autifyhq/autify-cli/0.29.0 linux-x64 node-v18.15.0"
else
  echo "\$@"
fi
EOS
chmod +x "$DIR/autify/bin/autify"

cat << EOS > "$DIR/autify/bin/autify.cmd"
@ECHO OFF
IF %1==--version (ECHO @autifyhq/autify-cli/0.29.0 win32-x64 node-v18.15.0) ELSE ECHO %*
EOS

echo "$DIR/autify/bin" >> "$DIR/autify/path"
