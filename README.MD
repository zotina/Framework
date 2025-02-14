# Custom Framework - Documentation

## Introduction
This custom framework is designed to simplify the development of Java applications based on servlets. It provides annotations for managing routes, HTTP requests, sessions, uploaded files, as well as authentication and validation features.

---

## Configuration

### 1. Configuring the `web.xml` file
Add the following configuration to your `web.xml` file:

```xml
<servlet>
    <servlet-name>FrontController</servlet-name>
    <servlet-class>framework.FrontController</servlet-class>
    <init-param>
        <param-name>packageController</param-name>
        <param-value>[YourControllersPackageName]</param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>FrontController</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

- Replace `[YourControllersPackageName]` with the package containing your controllers.
- All controllers must be annotated with `@Controller`.
- To configure authentication, add:

```xml
<init-param>
    <param-name>auth</param-name>
    <param-value>{host_name}</param-value>
</init-param>
```

---

## Usage Guide

### Available Annotations

#### 1. `@Url`
Defines the URL access for a controller.

#### 2. `@Post` and `@Get`
Indicates the type of HTTP request (POST or GET) supported by a controller method.

#### 3. `@Param`
Allows manipulation of values sent to the controller via query parameters.

Example:
```java
@Param("id")
private int id;
```

#### 4. `@RestApi`
Annotate your REST API controllers with `@RestApi` to indicate that they return JSON responses.

---

## Session Management

There are two ways to handle sessions:

1. Using `@Param("session") Session` as a method argument.
2. Declaring a `Session` attribute in your controller:

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

## File Upload Management

To enable file uploads, use the `FileUpload` class:

```java
@Param("pdp")
FileUpload pdp;
```

The uploaded file will be retrieved as a `FileUpload` object.

---

## Data Validation

The framework supports the following validation annotations:
- `@Valid`: Validates an entire object
- `@Email`: Checks that the value is a valid email address
- `@NotNull`: Ensures that the value is not `null`
- `@Min(value)`: Sets a minimum value for a numeric field

Example:
```java
public class User {
    @NotNull
    private String username;
    
    @Email
    private String email;
}
```

---

## Authentication Management

You can restrict access to functions or classes using `@Auth("Role")`.

### At the Method Level
```java
@Auth("Admin")
public void adminOnlyFunction() {
    // Code accessible only to admins
}
```

### At the Class Level
```java
@Auth("User")
@Controller
public class UserController {
    // All methods in this class will be accessible only to users with the "User" role
}
```

---

## Contact and Support
If you have any questions or issues regarding the use of this custom framework, feel free to contact us at the following email address: **zotinafiti@gmail.com**.

