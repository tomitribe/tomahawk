# Tomahawk
Java agent that allows wireshark to decrypt TLS traffic that uses DH cipher suites

## Usage
Add the tomahawk agent to your JVM by adding the following to your `setenv.sh`:

```
export JAVA_OPTS="$JAVA_OPTS -javaagent:/PATH/TO/tomahawk-1.0.0-shaded.jar"
```

## Configuration

By default, the log file will be written to `~/ssl.log`. You can customize this by adding a system property `-DTomahawkAgent.logfile=/path/to/ssl.log` 

## Running

Look for the following log entries. The last log entry will appear upon the first TLS request to the JVM:

```
Fri Oct 20 11:13:46 CDT 2017 TomahawkAgent: TomahawkAgent installed. Will attempt to log TLS secrets
Fri Oct 20 11:14:09 CDT 2017 TomahawkAgent: Attempting to enhance 'sun.security.ssl.Handshaker'...
Fri Oct 20 11:14:09 CDT 2017 TomahawkAgent: '-DTomahawkAgent.logfile=...' property not defined, using '/Users/jonathanfisher/ssl.log' instead
```
