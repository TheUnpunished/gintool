# NFS GINTOOL

This tools allows for GIN files to be encoded in a more convenient and fast way. No HEX editing needed anymore. Learn more here: [NFSMods](https://nfsmods.xyz/mod/3499)

## Compiling from source

In order to compile gintool, you'll need JDK 11 and Maven. After that navigate to the folder with the source code, where _pom.xml_ is located and run:
```
mvn clean package
```
After that, compiled jar will be located in _target_ folder. Enjoy!

## Changelog

- 1.0: Initial release
- 1.1: UI Logic fixes
- 1.2: Added ability to encode deceleration GIN's, major performance improvements
- 1.2.1: Added failsaves for gin_encode and gin2
- 1.3 (initial commit): Major code refactoring. Fields now support decimals. Tool now uses/requires JDK/JRE 11 to be installed, potential accuracy improvements by porting algorithm of gin2.exe to java code, which also removes the requirement of gin2.exe, vcruntime140d.dll and ucrtbased.dll altogether
- 1.3.1: Small UI changes, improved .ini logic, algorithmic number of threads calculation
- 1.3.2: More UI changes, new fields for NFS Carbon and .ini stores more data
- 1.3.3: UI changes, multilanguage support, rewritten "Launch TMXTool" logic

## Contributing
If you want to contribute to the project (add a translation, etc.), don't hesitate to make pull requests. Example messages file can be seen here: [messages_en.properties](../main/src/main/resources/i18n/messages_en.properties)