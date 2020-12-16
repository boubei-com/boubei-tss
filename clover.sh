
mvn clean clover2:setup test clover2:aggregate clover2:clover

# 在maven的setting.xml的<pluginGroups>里加一行： <pluginGroup>com.atlassian.maven.plugins</pluginGroup>
