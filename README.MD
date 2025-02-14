# Framework Personnalisé - Documentation

## Introduction
Ce framework personnalisé a été conçu pour simplifier le développement d'applications Java basées sur des servlets. Il fournit des annotations pour la gestion des routes, des requêtes HTTP, des sessions, des fichiers uploadés, ainsi que des fonctionnalités d'authentification et de validation.

---

## Configuration

### 1. Configuration du fichier `web.xml`
Ajoutez la configuration suivante dans votre fichier `web.xml` :

```xml
<servlet>
    <servlet-name>FrontController</servlet-name>
    <servlet-class>framework.FrontController</servlet-class>
    <init-param>
        <param-name>packageController</param-name>
        <param-value>[NomDuPackageDeVosControllers]</param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>FrontController</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

- Remplacez `[NomDuPackageDeVosControllers]` par le package contenant vos contrôleurs.
- Tous les contrôleurs doivent être annotés avec `@Controller`.
- Pour configurer l'authentification, ajoutez :

```xml
<init-param>
    <param-name>auth</param-name>
    <param-value>{nom_hote}</param-value>
</init-param>
```

---

## Guide d'utilisation

### Annotations disponibles

#### 1. `@Url`
Permet de définir l'URL d'accès à un contrôleur.

#### 2. `@Post` et `@Get`
Indiquent le type de requête HTTP (POST ou GET) supporté par une méthode du contrôleur.

#### 3. `@Param`
Permet de manipuler les valeurs envoyées au contrôleur via des paramètres de requête.

Exemple :
```java
@Param("id")
private int id;
```

#### 4. `@RestApi`
Annotez vos contrôleurs REST API avec `@RestApi` pour indiquer qu'ils retournent des réponses JSON.

---

## Gestion des sessions

Deux possibilités s'offrent à vous pour manipuler les sessions :

1. En utilisant `@Param("session") Session` en argument de méthode.
2. En déclarant un attribut `Session` dans votre contrôleur :

```java
@Controller
public class LoginController {
    private Session session;

    public void setSession(Session session) {
        this.session = session;
    }
}
```

---

## Gestion des fichiers uploadés

Pour permettre l'upload de fichiers, utilisez la classe `FileUpload` :

```java
@Param("pdp")
FileUpload pdp;
```

Le fichier uploadé sera récupérable sous forme d'objet `FileUpload`.

---

## Validation des données

Le framework supporte les annotations de validation suivantes :
- `@Valid` : Valide un objet entier
- `@Email` : Vérifie que la valeur est une adresse email valide
- `@NotNull` : Assure que la valeur n'est pas `null`
- `@Min(value)` : Définit une valeur minimale pour un champ numérique

Exemple :
```java
public class User {
    @NotNull
    private String username;
    
    @Email
    private String email;
}
```

---

## Gestion de l'authentification

Vous pouvez restreindre l'accès aux fonctions ou aux classes en utilisant `@Auth("Role")`.

### Au niveau des méthodes
```java
@Auth("Admin")
public void adminOnlyFunction() {
    // Code accessible uniquement aux admins
}
```

### Au niveau des classes
```java
@Auth("User")
@Controller
public class UserController {
    // Toutes les méthodes de cette classe seront accessibles uniquement aux utilisateurs ayant le rôle "User"
}
```

---

## Contact et Support
Si vous avez des questions ou des problèmes concernant l'utilisation de ce framework personnalisé, n'hésitez pas à nous contacter à l'adresse email suivante : **zotinafiti@gmail.com**.

