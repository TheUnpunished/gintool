# NFS GINTOOL

This tools allows for GIN files to be encoded in a more convenient and fast way. No HEX editing needed anymore. Learn more here: [NFSMods](https://nfsmods.xyz/mod/3499)

## Compiling from source

In order to compiler gintool, you'll need JDK 11 and Maven. After that navigate to the folder with the source code, where _pom.xml_ is located and run:
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