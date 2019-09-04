FROM scratch
WORKDIR /files

COPY target/classes/logging.properties ./conf/
COPY target/orders.jar target/libs/* ./lib/
