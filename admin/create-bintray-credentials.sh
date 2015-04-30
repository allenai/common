if [ -z "$BINTRAY_API_KEY" ]; then
  echo "BINTRAY_API_KEY is not set or is empty!"
  exit 1
fi

mkdir -p ~/.bintray
chmod 700 ~/.bintray
cat > ~/.bintray/.credentials <<CREDS
realm = Bintray API Realm
host = api.bintray.com
user = ai2-dev
password = $BINTRAY_API_KEY
CREDS
