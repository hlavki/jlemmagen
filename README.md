# JLemmagen

JLemmaGen is java implmentation of [LemmaGen][lemmagen] project. It's open source lemmatizer with 15 prebuilted european lexicons.
Of course you can build your own lexicon.

[LemmaGen][lemmagen] project aims at providing standardized open source multilingual platform for lemmatisation.

Project contains 2 libraries:

*    **lemmagen.jar** - implementation of lemmatizer and API for building own lemmatizers
*    **lemmagen-lang.jar** - prebuilted lemmatizers from [Multext Eastern dictionaries][multeast]
    * **IMPORTANT!**  - see [License](#markdown-header-license) chapter.

### Sample Usage
    Lemmatizer lm = LemmatizerFactory.getPrebuilt("mlteast-en");
    assert("be".equals(lm.lemmatize("are")));

### Maven
Repository:

    <repository>
        <id>jlemmagen-snapshots</id>
        <name>JLemmaGen snaphsot repository</name>
        <url>https://repository.xit.camp/repository/maven-public-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <layout>default</layout>
    </repository>

Dependency:

    <dependency>
        <groupId>eu.hlavki.text</groupId>
        <artifactId>lemmagen-lang</groupId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

### Lucene (Solr)
You need these jars to integrate with lucene/solr:

*    lemmagen-lucene.jar
*    lemmagen.jar
*    lemmagen-lang.jar
*    SLF4J API and implememtation (e.g. slf4j-jdk14.jar)

Example of solr filter definition in schema (e.g. Slovak):

    <filter class="org.apache.lucene.analysis.lemmagen.LemmagenFilterFactory" lexicon="mlteast-sk"/>


### License

All source code is licensed under Apache License 2.0. Important note is that binary rule tree files (*.lem) are **NOT** licensed under Apache License 2.0 and can be used only for non-commercial projects.

[lemmagen]: http://lemmatise.ijs.si/Software/Version3
[multeast]: http://nl.ijs.si/ME/V4/
