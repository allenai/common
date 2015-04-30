mkdir -p ~/.bintray
chmod 700 ~/.bintray
cat > ~/.bintray/.credentials <<CREDS
realm = Bintray API Realm
host = api.bintray.com
user = ai2-dev
password = $BINTRAY_API_KEY
CREDS
