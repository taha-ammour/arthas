# Projet-Gl
# Rapport d'amélioration du projet : Partie 2

### Projet Arthas - Module Core - Java Diagnostic Tool
- **Fait par** : AMMOUR Mohamed Taha
- **Lien vers le projet** : [https://github.com/taha-ammour/arthas](https://github.com/taha-ammour/arthas)
- **Version analysée** : `4.1.5` - tag `arthas-all-4.1.5` - commit [`8c413e2`](https://github.com/alibaba/arthas/commit/8c413e2) - publiée le 10 janvier 2026
- **Compatibilité** : Le projet est désormais compatible avec Java 11+ (testé avec JDK 21 et JDK 23)

---

### 1. Remplacement de nombres magiques - ClassLoaderCommand.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/command/klass100/ClassLoaderCommand.java`

**Situation existante** : La classe utilise des valeurs littérales codées en dur à de multiples endroits. La valeur `-1` apparaît ~20 fois comme code d'erreur dans des appels à `process.end(-1, ...)`. La valeur `256` est utilisée comme taille de page, et `0x3FFF` comme intervalle de vérification d'interruption. Ces valeurs ne sont pas explicites et leur signification ne peut être comprise qu'en lisant le contexte environnant.

**Modification apportée** : Trois constantes nommées ont été ajoutées en haut de la classe :

    public static final int PAGE_SIZE = 256;
    public static final int CODE_ERROR = -1;
    private static final int INTERRUPT_CHECK_INTERVAL = 0x3FFF;

Toutes les occurrences des valeurs littérales correspondantes ont été remplacées par ces constantes.

**En quoi c'est mieux** : Le code est plus lisible on comprend immédiatement que `CODE_ERROR` représente un code d'erreur et que `PAGE_SIZE` définit la taille d'une page de résultats. Si ces valeurs doivent changer, une seule modification suffit au lieu de ~20. Cela réduit aussi le risque d'incohérence si un développeur oublie de modifier une occurrence.

---

### 2. Remplacement de nombres magiques - HttpApiHandler.java (ApiTerm)

**Fichier** : `core/src/main/java/com/taobao/arthas/core/shell/term/impl/http/api/HttpApiHandler.java`

**Situation existante** : La classe interne `ApiTerm` retourne des valeurs littérales `1000` et `200` dans les méthodes `width()` et `height()`, sans explication de leur signification.

**Modification apportée** :

    AVANT :
        public int width() { return 1000; }
        public int height() { return 200; }

    APRÈS :
        public static final int DEFAULT_WIDTH = 1000;
        public static final int DEFAULT_HEIGHT = 200;
        public int width() { return DEFAULT_WIDTH; }
        public int height() { return DEFAULT_HEIGHT; }

**En quoi c'est mieux** : Les constantes nommées rendent explicite la signification de ces dimensions (largeur et hauteur par défaut du terminal API). Elles sont modifiables en un seul endroit.

---

### 3. Renommage de méthode - Ansi.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/view/Ansi.java`

**Situation existante** : La méthode `a(Attribute)` a un nom non descriptif d'un seul caractère. Il est impossible de comprendre son rôle sans lire son implémentation. D'autres surcharges de `a()` existent (pour String, int, char, etc.) et sont des méthodes d'appending mais celle-ci a un comportement différent : elle ajoute un attribut ANSI à une liste d'options qui seront flush plus tard.

**Modification apportée** :

    AVANT : public Ansi a(Attribute attribute)
    APRÈS : public Ansi applyAttribute(Attribute attribute)

**En quoi c'est mieux** : Le nom `applyAttribute()` décrit précisément ce que fait la méthode. Cela respecte la convention Java de noms de méthodes descriptifs et élimine toute ambiguïté avec les autres surcharges de `a()`.

---

### 4. Renommage de variable - ClassLoaderCommand.java (clazz à un noms descriptifs)

**Fichier** : `core/src/main/java/com/taobao/arthas/core/command/klass100/ClassLoaderCommand.java`

**Situation existante** : Le nom `clazz` est utilisé dans 7 méthodes différentes comme convention pour éviter le mot-clé `class`. C'est un anti-pattern courant en Java qui réduit la lisibilité le nom ne donne aucune information sur le rôle de la variable.

**Modification apportée** : Chaque occurrence a été renommée selon son contexte :

    - Boucles d'itération sur les classes chargées : clazz à loadedClass
    - Résultat de loadClass() : clazz à resultClass
    - Paramètre de codeSourceLocation() : clazz à targetClass

Méthodes modifiées : `getAllClasses()`, `getAllClassLoaderInfo()`, `getAllClassLoaders()`, `urlStats()`, `processUrlClasses()`, `codeSourceLocation()`, `processLoadClass()`.

**En quoi c'est mieux** : Chaque variable porte un nom qui décrit précisément son rôle dans le contexte. Un développeur lisant `loadedClass` comprend immédiatement qu'il s'agit d'une classe chargée par le ClassLoader, là où `clazz` ne communique rien.

---

### 5. Renommage de champ et méthodes - RowAffect.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/util/affect/RowAffect.java`

**Situation existante** : Le champ `rCnt` et les méthodes `rCnt()` / `rCnt(int mc)` utilisent des noms cryptiques. Le paramètre `mc` est également non descriptif. Ces noms violent la convention camelCase descriptive de Java.

**Modification apportée** (via refactoring IntelliJ, tous les appelants mis à jour automatiquement) :

    AVANT :
        private final AtomicInteger rCnt = new AtomicInteger();
        public int rCnt(int mc) { return rCnt.addAndGet(mc); }
        public int rCnt() { return rCnt.get(); }

    APRÈS :
        private final AtomicInteger rowCount = new AtomicInteger();
        public int addRowCount(int count) { return rowCount.addAndGet(count); }
        public int getRowCount() { return rowCount.get(); }

Le constructeur `RowAffect(int rCnt)` à `RowAffect(int initialCount)`.

**En quoi c'est mieux** : `rowCount` indique clairement qu'il s'agit d'un compteur de lignes affectées. `getRowCount()` et `addRowCount()` suivent la convention JavaBean standard. Tous les fichiers qui appelaient `rCnt()` ont été mis à jour, garantissant la cohérence du projet entier.

---

### 6. Modernisation de syntaxe - ClassLoaderCommand.java (Comparator)

**Fichier** : `core/src/main/java/com/taobao/arthas/core/command/klass100/ClassLoaderCommand.java`

**Situation existante** : Un Comparator est défini via une classe anonyme de 5 lignes pour simplement comparer des noms de classes :

    classSet = new TreeSet<Class<?>>(new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    });

**Modification apportée** :

    classSet = new TreeSet<>(Comparator.comparing(Class::getName));

**En quoi c'est mieux** : La référence de méthode Java 8+ est plus concise, plus lisible et exprime directement l'intention : " trier par nom ". Cela a été rendu possible par la migration vers Java 11.

---

### 7. Suppression de sun.misc.Unsafe - ClassLoaderUtils.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/util/ClassLoaderUtils.java`

**Situation existante** : La méthode `getUrls()` utilise `sun.misc.Unsafe` pour lire des champs privés du ClassLoader JDK 9+. Elle obtient `Unsafe` via réflexion, puis utilise `objectFieldOffset()` et `getObject()` pour contourner l'encapsulation. `sun.misc.Unsafe` est une API interne propriétaire, non documentée, et qui génère des erreurs sur JDK 9+.

**Modification apportée** : Remplacement de l'approche Unsafe par la réflexion standard Java :

    AVANT :
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
        long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
        Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

    APRÈS :
        ucpField.setAccessible(true);
        Object ucpObject = ucpField.get(classLoader);


**En quoi c'est mieux** : Le code n'utilise plus aucune API propriétaire. `field.setAccessible(true)` + `field.get()` est l'API standard Java pour lire des champs privés. Le code compile sans warning sur tout JDK 11+ et ne risque plus de casser lors d'une mise à jour du JDK.

---

### 8. Suppression de sun.misc.Unsafe - UnsafeUtils.java

**Fichier** : `common/src/main/java/com/taobao/arthas/common/UnsafeUtils.java`

**Situation existante** : La classe importait directement `sun.misc.Unsafe` et exposait un champ `public static final Unsafe UNSAFE`. Sur les JDK des machines universitaires, cela provoquait une erreur de compilation fatale : " package sun.misc does not exist ".

**Modification apportée** : Suppression complète de l'import, du champ `UNSAFE`, et du bloc `static {}`. La méthode `implLookup()` utilise désormais `field.setAccessible(true)` + `field.get(null)` avec un fallback vers `MethodHandles.lookup()`.

    AVANT :
        import sun.misc.Unsafe;
        public static final Unsafe UNSAFE;
        static { ... UNSAFE = unsafe; }
        // implLookup() using UNSAFE.staticFieldOffset / getObject

    APRÈS :
        // No import of sun.misc.Unsafe
        public static MethodHandles.Lookup implLookup() {
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            implLookupField.setAccessible(true);
            IMPL_LOOKUP = (MethodHandles.Lookup) implLookupField.get(null);
        }

**En quoi c'est mieux** : Le build qui échouait à l'université avec " package sun.misc does not exist " compile désormais. La classe n'a plus aucune dépendance compile-time vers des APIs internes du JDK.

---

### 9. Adaptation GlobalOptions.java - Champ static final

**Fichier** : `core/src/main/java/com/taobao/arthas/core/GlobalOptions.java`

**Situation existante** : La méthode `updateOnglStrict()` utilisait `UnsafeUtils.UNSAFE` (le champ qu'on a supprimé dans 8). Après la suppression de `UNSAFE`, le build échouait avec " cannot find symbol: variable UNSAFE ".

**Modification apportée** : Accès à Unsafe via `Class.forName("sun.misc.Unsafe")` au lieu d'un import direct, puis invocation via réflexion (`Method.invoke`). Cela est nécessaire car `field.set()` ne fonctionne pas sur les champs `static final` en JDK 12+.

    APRÈS :
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        // invoke staticFieldBase, staticFieldOffset, putBoolean via Method

**En quoi c'est mieux** : Aucune dépendance compile-time vers `sun.misc.Unsafe`. Le code compile sur n'importe quel JDK 11+ sans erreur ni warning d'import. L'accès se fait uniquement au runtime, et si Unsafe n'est pas disponible, l'exception est attrapée silencieusement.

---

### 10. Migration du build Java 8 à Java 11

**Fichiers** : `pom.xml` (racine) et `core/pom.xml`

**Situation existante** : Le projet ciblait Java 8 (`<source>8</source>`, `<target>8</target>`). Sur les JDK récents (21, 23), cela produisait le warning " source value 8 is obsolete and will be removed in a future release ". Plus important, sur les machines universitaires, les APIs internes du JDK (`com.sun.tools.attach`, `jdk.jfr`, `sun.management`) n'étaient pas accessibles, causant des erreurs de compilation fatales.

**Modification apportée dans pom.xml (racine)** :
- `<maven.compiler.source>1.8</maven.compiler.source>` à `11`
- `<maven.compiler.target>1.8</maven.compiler.target>` à `11`
- Plugin `maven-compiler-plugin` : `<source>8</source>` à `11`
- Plugin `maven-javadoc-plugin` : `<source>1.8</source>` à `11`

**Modification apportée dans core/pom.xml** :
- Ajout de `--add-modules jdk.attach,jdk.jfr` pour rendre visibles les modules JDK nécessaires
- Ajout de `--add-exports` pour `sun.management`, `sun.management.counter`, `sun.management.counter.perf`
- Ajout du plugin `maven-surefire-plugin` avec les mêmes flags pour les tests
- Suppression de la dépendance obsolète `maven-jdk-tools-wrapper` (inutile en Java 11+)

**En quoi c'est mieux** : Le build compile et tous les 173 tests passent sur JDK 11, 21 et 23. Le warning " source value 8 is obsolete " disparaît. Le projet est prêt pour les JDK modernes tout en conservant sa compatibilité avec les APIs internes nécessaires au fonctionnement d'Arthas (outil de diagnostic JVM).

---

### 11. Correction d'un test qui échouait - ObjectViewTest.java

**Fichier** : `core/src/test/java/com/taobao/arthas/core/view/ObjectViewTest.java`

**Situation existante** : Le test `testDate()` échouait systématiquement sur les machines dont le fuseau horaire n'est pas GMT+8. Le code original utilisait `getRawOffset()` pour calculer le décalage horaire, ce qui ne tient pas compte de l'heure d'été (DST). Sur une machine en Europe (GMT+1/GMT+2), le test produisait une date différente de celle attendue.

**Modification apportée** :

    AVANT :
        Date d = new Date(1531204354961L - TimeZone.getDefault().getRawOffset()
                        + TimeZone.getTimeZone("GMT+8").getRawOffset());

    APRÈS :
        long timestamp = 1531204354961L;
        TimeZone local = TimeZone.getDefault();
        TimeZone target = TimeZone.getTimeZone("GMT+8");
        Date d = new Date(
                timestamp
                        - local.getOffset(timestamp)
                        + target.getOffset(timestamp)
        );

**En quoi c'est mieux** : `getOffset(timestamp)` retourne le décalage réel à la date donnée (incluant l'heure d'été si applicable), contrairement à `getRawOffset()` qui retourne uniquement le décalage de base. Le test passe désormais quel que soit le fuseau horaire de la machine. De plus, l'extraction de `timestamp` en variable nommée améliore la lisibilité.

---

## Comment compiler et exécuter

### Prérequis
- JDK 11 ou supérieur (testé avec JDK 21 et JDK 23)
- Maven installé
- JAVA_HOME configuré

### Compiler le projet
    mvn clean install -DskipTests -pl !arthas-vmtool
    Résultat attendu : BUILD SUCCESS (22/22 modules)

### Exécuter les tests
    mvn test -pl core
    Résultat attendu : Tests run: 173, Failures: 0, Errors: 0, Skipped: 7

### Lancer Arthas (build local)
    TERMINAL 1 :
        java -jar math-game\target\math-game.jar

    TERMINAL 2 :
        java -jar boot\target\arthas-boot-jar-with-dependencies.jar --arthas-home packaging\target\arthas-bin

    Sélectionner le processus math-game (ex: taper 1).
    Vérifier que la bannière affiche "version 4.1.5" (build local).
    Commandes utiles : dashboard, thread, quit, stop.