#!/usr/bin/env sh
echo "Starting NGINX"
export EXISTING_VARS=$(printenv | awk -F= '{print $1}' | sed 's/^/\$/g' | paste -sd,);

echo "==============="
echo "ENVIRONMENT VARIABLES:"
printenv
echo "==============="

for file in $JSFOLDER;
do
  echo "Substitute environment variables in $file"

  tmp_file="$file.tmp"
  echo "envsubstitute $file to $tmp_file"
  envsubst $EXISTING_VARS < $file > $tmp_file

  echo "replace file ($file) by tmp file ($tmp_file)"
  rm $file
  mv $tmp_file $file

  echo -e "\r\n"
done

nginx_conf="/etc/nginx/nginx.conf"
tmp_nginx_conf="$nginx_conf.tmp"

echo "envsubstitute $nginx_conf to $tmp_file"
envsubst $EXISTING_VARS < $nginx_conf > $tmp_nginx_conf

echo "replace config ($nginx_conf) by tmp file ($tmp_nginx_conf)"
rm $nginx_conf
mv $tmp_nginx_conf $nginx_conf

nginx -g 'daemon off;'
