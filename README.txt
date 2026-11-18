AutoWystaw - mod kliencki Fabric dla Minecraft 1.21.4
Wystawia automatycznie Splash Potion of Weakness (04:00) komenda /wystaw 1 160

BUDOWANIE
1. Zainstaluj JDK 21.
2. Dodaj Gradle wrapper (jednorazowo): w tym folderze uruchom "gradle wrapper --gradle-version 8.12"
   albo skopiuj pliki gradlew, gradlew.bat i folder gradle/ z https://fabricmc.net/develop/template/
3. Uruchom: ./gradlew build   (Windows: gradlew.bat build)
4. Gotowy jar: build/libs/autowystaw-1.0.0.jar (nie ten z "-sources")
5. Wrzuc jar do .minecraft/mods/ razem z Fabric Loader i Fabric API 1.21.4.

UZYCIE
K albo /autowystaw        - wlacz/wylacz
/autowystaw cmd wystaw 1 200 - zmiana komendy (zapisuje sie w config/autowystaw.txt)
/autowystaw status        - podglad

UWAGA: sprawdz regulamin serwera - automatyzacja moze byc zabroniona.
