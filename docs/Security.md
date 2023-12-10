# Serving with HTTPS

Serving with SSL is available for Docker and Helm Deployment. In local deployment directly with mvn (backend) and 
npm (frontend) it can be configured, too. 

For docker configurations, see below. For helm, additionally set the related ingress (frontend, backend) as needed to 
enabled and configure it.

## Frontend

The Frontend uses a nginx-unprivileged image restricting access heavily. One can use the following configuration as a
starting point.

Let's assume the following structure:
```shell
ls
>> /
>> /ssl-certificates
>> /ssl-certificates/localhost.crt
>> /ssl-certificates/localhost.key
>> /nginx.conf
```

For testing purposes, create self-signed certificates:
``` sh
mkdir ssl-certificates
cd ssl-certificates

openssl req -x509 -out localhost.crt -keyout localhost.key \
  -newkey rsa:2048 -nodes -sha256 \
  -subj '/CN=localhost' -extensions EXT -config <( \
   printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
```
_NOTE: For productive use, you can use certificates provided by a Certificate Authority._


Create a nginx.conf to provide certificates for listening on 443 for tls. You can find an example 
[here](../frontend/nginx.conf).
``` conf
http {
    # other configurations 
    server {
        listen 443 ssl;
        server_name local-puris-frontend.com;

        ssl_certificate /etc/nginx/ssl/localhost.crt;
        ssl_certificate_key /etc/nginx/ssl/localhost.key;
        
        # TLS version >= 1.2
        ssl_protocols TLSv1.2 TLSv1.3;

        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
    }
}
```

Start the docker image mounting the certificates and the nginx.conf as follows:
``` sh

docker run --rm --name frontend \
  -v $(pwd)/ssl-certificates:/etc/nginx/ssl \
  -v $(pwd)/nginx.conf:/etc/nginx/nginx.conf \
  puris-frontend:dev
>> exposes at 8080, 443
```

If you want to use of the dns alias for localhost:443, make sure to edit your /etc/hosts file:
```sh
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <container_name_or_id>

sudo vim /etc/hosts
>>add entry like 172.17.0.2 local-puris-frontend.com
# :wq! (write and quit)
```

