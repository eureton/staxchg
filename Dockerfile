FROM clojure:lein
RUN apt-get update \
    && apt-get install -y procps skylighting
WORKDIR /workspace
ENTRYPOINT [ "/bin/bash", "-c", "while sleep 1000; do :; done" ]
