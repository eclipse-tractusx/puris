#!/usr/bin/env sh
echo "Starting NGINX"
export EXISTING_VARS=$(printenv | awk -F= '{print $1}' | sed 's/^/\$/g' | paste -sd,);

echo "==============="
echo "ENVIRONMENT VARIABLES:"
printenv
echo "==============="

for file in $JSFOLDER;
do
  echo -e "\r\n"
  echo $file

  # other command cuts off length of files. sponge has no file-size limitation.
  #cat $file | envsubst $EXISTING_VARS | tee $file
  #envsubst $EXISTING_VARS < $file | tee $file
  envsubst $EXISTING_VARS < $file | sponge $file
  echo -e "\r\n"
done

envsubst $EXISTING_VARS < /etc/nginx/nginx.conf | sponge /etc/nginx/nginx.conf

nginx -g 'daemon off;'
