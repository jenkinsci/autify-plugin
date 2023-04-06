#!/bin/bash
DIR=$PWD
mkdir -p "$DIR/autify/bin"

cat << EOS > "$DIR/autify/bin/autify"
#!/bin/bash
echo "\$@"
EOS
chmod +x "$DIR/autify/bin/autify"

echo "$DIR/autify/bin" >> "$DIR/autify/path"
