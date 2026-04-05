# Projet-Gl
# Rapport d'amélioration du projet : Partie 2

### Projet Arthas - Module Core - Java Diagnostic Tool
- **Fait par** : AMMOUR Mohamed Taha
- **Lien vers le projet** : [https://github.com/taha-ammour/arthas](https://github.com/taha-ammour/arthas)
- **Version analysée** : `4.1.5` - tag `arthas-all-4.1.5` - commit [`8c413e2`](https://github.com/alibaba/arthas/commit/8c413e2) - publiée le 10 janvier 2026
- **Compatibilité** : Le projet reste compatible avec Java 8 et supérieur (testé avec JDK 8, 11, 21 et 23). Les flags JPMS nécessaires sont activés automatiquement via un profil Maven sur JDK 11+.

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

**En quoi c'est mieux** : La référence de méthode Java 8+ est plus concise, plus lisible et exprime directement l'intention : " trier par nom ".

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


**En quoi c'est mieux** : Le code n'utilise plus aucune API propriétaire. `field.setAccessible(true)` + `field.get()` est l'API standard Java pour lire des champs privés. Le code compile sans warning sur tout JDK 8+ et ne risque plus de casser lors d'une mise à jour du JDK.

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

**En quoi c'est mieux** : Aucune dépendance compile-time vers `sun.misc.Unsafe`. Le code compile sur n'importe quel JDK 8+ sans erreur ni warning d'import. L'accès se fait uniquement au runtime, et si Unsafe n'est pas disponible, l'exception est attrapée silencieusement.

---

### 10. Configuration JPMS conditionnelle via profil Maven

**Fichiers** : `core/pom.xml`

**Situation existante** : Le projet ciblait Java 8 (`<source>8</source>`, `<target>8</target>`). Sur les JDK récents (11+), le système de modules JPMS empêche l'accès aux APIs internes du JDK (`com.sun.tools.attach`, `jdk.jfr`, `sun.management`), causant des erreurs de compilation fatales sur les machines universitaires. Il fallait ajouter des flags `--add-modules` et `--add-exports`, mais ces flags n'existent pas en Java 8 et provoquent une erreur : " option --add-modules not allowed with target 8 ".

**Modification apportée dans core/pom.xml** :
- Les flags JPMS (`--add-modules`, `--add-exports`, `--add-opens`) ont été placés dans un profil Maven `jdk11-plus` qui ne s'active que sur JDK 11 et supérieur via `<activation><jdk>[11,)</jdk></activation>`
- Le profil surcharge `<source>` et `<target>` à `11` et désactive le flag `--release` (qui bloquerait `--add-exports`) via `<release combine.self="override"/>`
- Le `maven-surefire-plugin` reçoit les mêmes flags JPMS dans le profil pour que les tests accèdent aux modules internes
- La dépendance `maven-jdk-tools-wrapper` est conservée pour assurer la compatibilité Java 8 (elle fournit `tools.jar` au classpath sur JDK 8, et est ignorée sur JDK 11+)

**En quoi c'est mieux** : Le build fonctionne sur Java 8 ET Java 11+ sans aucune modification manuelle. Maven détecte automatiquement la version du JDK et active le profil si nécessaire. Le CI GitHub Actions (qui teste sur JDK 8, 11, 17, 21 et 25) passe sur toutes les versions. Le projet conserve la compatibilité descendante avec Java 8 tout en supportant le système de modules JPMS sur les JDK modernes.

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

### 12. Suppression de la méthode dépréciée `restorCursorPosition()` - Ansi.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/view/Ansi.java`

**Situation existante** : La méthode `restorCursorPosition()` (sans le 'e' dans "restore") est marquée `@Deprecated` et coexiste avec la version corrigée `restoreCursorPosition()`. Les deux méthodes font exactement la même chose : `return appendEscapeSequence('u')`. Ce problème avait déjà été identifié dans notre audit Partie 1 (section 4.3 Dépréciation). La méthode dépréciée est présente deux fois : dans la classe principale `Ansi` et dans la sous-classe interne `NoAnsi` qui l'override.

**Modification apportée** :

Suppression dans la classe `Ansi` :

    @Deprecated
    public Ansi restorCursorPosition() {
        return appendEscapeSequence('u');
    }


Suppression aussi dans la sous-classe `NoAnsi` :

**En quoi c'est mieux** : Élimine du code mort (méthode dépréciée identique à sa remplaçante). La classe perd 2 méthodes inutiles et les annotations `@Deprecated` associées. Tout appelant externe utilisant encore l'ancien nom recevra désormais une erreur de compilation claire, l'obligeant à corriger la typo, au lieu de silencieusement utiliser une méthode marquée comme obsolète.

---

### 13. Réduction de la complexité de `process()` - ProfilerCommand.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/command/monitor200/ProfilerCommand.java`

**Situation existante** : La méthode `process()` de `ProfilerCommand` concentre une logique dans une chaîne de if/else. Chaque branche contient sa propre logique inline : validation d'arguments, construction d'arguments, gestion de fichiers de sortie, etc.

**Modification apportée** : Extraction de 5 méthodes privées depuis `process()` :

    - processExecuteAction(asyncProfiler, process)   : gère l'action "execute"
    - processStart(asyncProfiler, process)            : gère l'action "start" 
    - processDumpCollapsed(asyncProfiler, process)    : gère l'action "dumpCollapsed"
    - processDumpFlat(asyncProfiler, process)         : gère l'action "dumpFlat"
    - processDumpTraces(asyncProfiler, process)       : gère l'action "dumpTraces"

La méthode `process()` ne contient plus que le handler qui lui va appeler la fonction selon l'action, sans logique métier inline.

**En quoi c'est mieux** : La complexité cyclomatique de `process()` diminue. Chaque méthode extraite a une complexité de 3-5 et une seule responsabilité. Le code est plus lisible : `process()` agit désormais comme un dispatcheur clair, et la logique métier de chaque action est isolée dans sa propre méthode, facilitant la maintenance et le debugging.

---

### 14. Suppression de surcharges redondantes de `applyAttribute()` - Ansi.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/view/Ansi.java`

**Situation existante** : La classe `Ansi` contient 11 surcharges de la méthode `applyAttribute()`. En dehors de `applyAttribute(Attribute)` qui applique un attribut ANSI, les 10 autres ne font qu'appeler `flushAttributes(); builder.append(value); return this;` avec des types différents (String, boolean, char, double, CharSequence, StringBuffer, etc.). Or, la surcharge générique `<T> applyAttribute(T value)` couvre déjà la plupart de ces cas grâce au mécanisme d'autoboxing de Java (les primitives boolean, char, double sont automatiquement converties en Boolean, Character, Double) et au polymorphisme (String, CharSequence, StringBuffer sont des sous-types de Object). Six méthodes sont donc des duplications inutiles de la version générique.

**Modification apportée** : Suppression de 6 surcharges redondantes couvertes par la version générique `<T>` :

    SUPPRIMÉ (6 méthodes) :
        public Ansi applyAttribute(String value) { flushAttributes(); builder.append(value); return this; }
        public Ansi applyAttribute(boolean value) { flushAttributes(); builder.append(value); return this; }
        public Ansi applyAttribute(char value) { flushAttributes(); builder.append(value); return this; }
        public Ansi applyAttribute(double value) { flushAttributes(); builder.append(value); return this; }
        public Ansi applyAttribute(CharSequence value) { flushAttributes(); builder.append(value); return this; }
        public Ansi applyAttribute(StringBuffer value) { flushAttributes(); builder.append(value); return this; }

    CONSERVÉ (5 méthodes, non redondantes) :
        public Ansi applyAttribute(Attribute attribute)                   // applique un vrai attribut ANSI
        public <T> Ansi applyAttribute(T value)                           // générique, remplace les 6 supprimées
        public Ansi applyAttribute(char[] value)                          // nécessaire car append(Object) sur char[] afficherait "[C@hashcode"
        public Ansi applyAttribute(char[] value, int offset, int len)     // multi-paramètres, non couverte
        public Ansi applyAttribute(CharSequence value, int start, int end) // multi-paramètres, non couverte

**Impact sur les autres fichiers** : Aucun. Les appelants existants (ex: `ansi.applyAttribute("hello")`) continuent de fonctionner de façon identique car le compilateur Java résout automatiquement vers la surcharge générique `<T>`. Le mécanisme d'autoboxing convertit les primitives (boolean -> Boolean, char -> Character, double -> Double) en objets compatibles avec `<T>`. Le comportement est strictement identique car `StringBuilder.append(Object)` appelle `toString()`, produisant le même résultat que les surcharges spécifiques.

**En quoi c'est mieux** : Élimine 6 méthodes dupliquées qui étaient identiques à la version générique. Le code respecte le principe DRY (Don't Repeat Yourself).

---

### 15. Réduction de la complexité de `processRequest()` - HttpApiHandler.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/shell/term/impl/http/api/HttpApiHandler.java`

**Situation existante** : La méthode `processRequest()` de `HttpApiHandler` mélange trois responsabilités distinctes : le parsing de l'action, la résolution/création de session, et le dispatching vers les handlers, ce qui rend la méthode difficile à lire et à tester.

**Modification apportée** : Extraction de 2 méthodes privées :

    - `resolveOrCreateSession(ctx, apiRequest, action)` : encapsule la résolution de session existante ou la création d'une session one-time, puis applique les attributs HTTP et le userId
    - `applyHttpSessionAttributes(ctx, session)` : transfère les attributs (Subject, userId) de la HttpSession vers la session Arthas


**En quoi c'est mieux** : La méthode `processRequest()` se lit désormais comme un scénario clair : parser l'action, résoudre la session, dispatcher la requête. La logique de résolution de session est isolée dans `resolveOrCreateSession()` avec une seule responsabilité. La logique de transfert des attributs HTTP est isolée dans `applyHttpSessionAttributes()`.

---

### 16. Remplacement de codes d'erreur par des exceptions - ProfilerCommand.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/command/monitor200/ProfilerCommand.java`

**Situation existante** : Les méthodes `processExecuteAction()` et `processDumpCollapsed()` gèrent les erreurs de validation en appelant directement `process.end(1, "message")` (code d'erreur) suivi d'un `return`. Ce pattern mélange la gestion des erreurs avec le flux de contrôle et oblige chaque méthode à connaître l'API `CommandProcess`. Or, la méthode appelante `process()` possède déjà un bloc `catch (Throwable e)` qui appelle `process.end(1, "AsyncProfiler error: " + e.getMessage())`, ce qui rend les codes d'erreur locaux redondants.

**Modification apportée** :

    AVANT (processExecuteAction) :
        if (actionArg == null) {
            process.end(1, "actionArg can not be empty.");
            return;
        }

    APRÈS :
        if (actionArg == null) {
            throw new IllegalArgumentException("actionArg can not be empty.");
        }

    AVANT (processDumpCollapsed) :
        if ("TOTAL".equals(actionArg) || "SAMPLES".equals(actionArg)) {
            String result = asyncProfiler.dumpCollapsed(Counter.valueOf(actionArg));
            appendExecuteResult(process, result);
        } else {
            process.end(1, "ERROR: dumpCollapsed argumment should be TOTAL or SAMPLES.");
        }

    APRÈS :
        if (!"TOTAL".equals(actionArg) && !"SAMPLES".equals(actionArg)) {
            throw new IllegalArgumentException("dumpCollapsed argument should be TOTAL or SAMPLES.");
        }
        String result = asyncProfiler.dumpCollapsed(Counter.valueOf(actionArg));
        appendExecuteResult(process, result);

**En quoi c'est mieux** : Les méthodes n'ont plus besoin de connaître l'API `CommandProcess` pour gérer les erreurs, ce qui réduit le couplage. Le flux d'erreur est unifié : toutes les erreurs remontent via le mécanisme standard d'exceptions Java et sont attrapées au même endroit dans `process()`. Le code est plus lisible : le cas d'erreur est traité en premier (early throw / guard clause) au lieu d'être imbriqué dans un if/else. Le `return` après `process.end()` — facile à oublier et source de bugs — est éliminé.

---

### 17. Décomposition de `outputFile()`- ProfilerCommand.java

**Fichier** : `core/src/main/java/com/taobao/arthas/core/command/monitor200/ProfilerCommand.java`

**Situation existante** : La méthode `outputFile()` modifie l'état de l'objet en assignant `this.file` (effet de bord / commande) et retourne simultanément la valeur de `this.file` (query). Ce mélange de responsabilités rend le comportement difficile à prédire : on ne s'attend pas à ce qu'un getter modifie l'état interne. De plus, la logique de génération du chemin de fichier est imbriquée dans la vérification de nullité.

**Modification apportée** : Décomposition en deux méthodes distinctes :

    - `ensureOutputFile()`: vérifie si `this.file` est null et le génère si nécessaire. Ne retourne rien.
    - `generateOutputFilePath()` : génère et retourne un chemin sans modifier l'état.

    AVANT :
        private String outputFile() throws IOException {
            if (this.file == null) {
                String fileExt = outputFileExt();
                File outputPath = ArthasBootstrap.getInstance().getOutputPath();
                if (outputPath != null) {
                    this.file = new File(outputPath, ...).getAbsolutePath();
                } else {
                    this.file = File.createTempFile(...).getAbsolutePath();
                }
            }
            return file;
        }

        // Appelant :
        final String outputFile = outputFile();

    APRÈS :
        private void ensureOutputFile() throws IOException {
            if (this.file == null) {
                this.file = generateOutputFilePath();
            }
        }

        private String generateOutputFilePath() throws IOException {
            String fileExt = outputFileExt();
            File outputPath = ArthasBootstrap.getInstance().getOutputPath();
            if (outputPath != null) {
                return new File(outputPath, ...).getAbsolutePath();
            } else {
                return File.createTempFile(...).getAbsolutePath();
            }
        }

        // Appelant :
        ensureOutputFile();
        final String outputFile = this.file;

**En quoi c'est mieux** : `ensureOutputFile()` clairement modifie l'état, ne retourne rien. `generateOutputFilePath()` clairement retourne un résultat, ne modifie rien. Un appelant qui lit `ensureOutputFile()` comprend immédiatement qu'il y a un effet de bord, contrairement à l'ancien `outputFile()` qui ressemblait à un getter. `generateOutputFilePath()` est réutilisable indépendamment.

---

## Comment compiler et exécuter

### Prérequis
- JDK 8 ou supérieur (testé avec JDK 8, 11, 21 et 23)
- Maven installé
- JAVA_HOME configuré

### Compiler le projet

    Windows :
        mvn clean install -DskipTests -pl !arthas-vmtool

    Linux / macOS :
        mvn clean install -DskipTests -pl '!arthas-vmtool'

    Résultat attendu : BUILD SUCCESS (22/22 modules)

    Note : le profil Maven jdk11-plus s'active automatiquement sur JDK 11+
    pour ajouter les flags JPMS nécessaires. Aucune option supplémentaire
    n'est requise.

### Exécuter les tests
    mvn test -pl core
    Résultat attendu : Tests run: 173, Failures: 0, Errors: 0, Skipped: 7

### Lancer Arthas (build local)

    TERMINAL 1 — Lancer l'application de démo :

        Windows :
            java -jar math-game\target\math-game.jar

        Linux / macOS :
            java -jar math-game/target/math-game.jar

    TERMINAL 2 — Lancer Arthas (build local) :

        Windows :
            java -jar boot\target\arthas-boot-jar-with-dependencies.jar --arthas-home packaging\target\arthas-bin

        Linux / macOS :
            java -jar boot/target/arthas-boot-jar-with-dependencies.jar --arthas-home packaging/target/arthas-bin

    Sélectionner le processus math-game (ex: taper 1).
    Vérifier que la bannière affiche "version 4.1.5" (build local).
    Commandes utiles : dashboard, thread, quit, stop.