# JiraMigration
Jira migration tool to transfer data between two Jira servers keeping the numbering, basic and custom fields.
There are 2 part:
1. Import from jira1 to local.
2. Export from local to jira2.

The following calls will do that:
JavaMigration.jar import fromKey toKey
JavaMigration.jar export fromKey toKey

SSL certificate could be required.
There are multiple ways to get it:
1. openssl s_client -showcerts -connect <www.jira.com>:443 </dev/null 2>/dev/null|openssl x509 -outform PEM >mycert.pem
2. Or export from the browser.

Certificate file could be imported to java with the following command:
keytool -importcert -alias <alias> -keystore "%JAVA_HOME%\lib\security\cacerts" -storepass changeit -file mycert.pem
