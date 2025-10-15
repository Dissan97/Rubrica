# Rubrica – Contact Management Application

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Build-Maven-blue)
![Swing](https://img.shields.io/badge/UI-JavaSwing-green)

This project is a **contact management application (Rubrica)** developed using **Java Swing**.
It allows users to manage personal contacts with support for both **filesystem-based** storage and **DBMS (MySQL)** storage.

> ⚠️ All source code and UI labels are written in **Italian**.

---

## 🧩 Features

* Manage contacts (create, update, delete, search)
* Dual storage system:

    * **Filesystem mode** (default)
    * **MySQL database mode**
* Simple graphical interface built with **Java Swing**
* Configurable runtime mode via `conf.properties`

---

## 🛠️ Build System

The project uses **Apache Maven** as its build system.

### Requirements

* **Java 21** (or higher)
* **Maven** installed and available in `PATH`

### Build Command

To compile and package the project:

```bash
mvn clean package
```

After a successful build, the executable JAR file will be available at:

```
target/Rubrica.jar
```

---

## 🚀 Running the Application

You can run the JAR directly from the terminal:

```bash
java -jar target/Rubrica.jar
```

Or move the file `Rubrica.jar` to any directory and launch it from there:

```bash
java -jar /path/to/Rubrica.jar
```

---

## 🗄️ Database Setup (Optional)

If you want to use **MySQL** as storage instead of the filesystem:

1. Execute the script `schema_database.sql` located in the project root
   to initialize the required database schema.

2. Create the following configuration file in the directory where the JAR is launched:

   ```
   conf/conf.properties
   ```

3. Example configuration file:

   ```properties
   # Storage mode: "dbms" for MySQL, or "fs" for filesystem (default)
   database.instance=dbms

   # MySQL connection details
   db.url=jdbc:mysql://addr:port/rubrica?useSSL=false&allowPublicKeyRetrieval=true&noAccessToProcedureBodies=true
   db.user=LOGIN
   db.password=login_pwd

   # Application user credentials
   db.user.logged=LOGGED
   db.user.login=LOGIN
   db.pass.login=login_pwd
   db.pass.logged=logged_pwd
   ```

---

## 📦 Notes

* The filesystem mode (`database.instance=fs`) stores data locally without requiring MySQL.
* The DBMS mode requires an accessible MySQL server and proper credentials.
* The application will automatically read the configuration file at startup.

---

## 🧑‍💻 Author

Developed by **Dissan97**
A simple and extensible contact manager written in Italian, powered by Java Swing.

---

## 🤝 Contributing

Contributions, suggestions, and improvements are welcome!
If you want to contribute:

1. Fork the repository
2. Create a new branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m "Add new feature"`)
4. Push to your branch (`git push origin feature/my-feature`)
5. Open a **Pull Request**

---

## 📜 License

This project is released under the **GNU General Public License v3.0 (GPLv3)**.  
You are free to use, modify, and distribute this software under the same license terms.

For more details, see the [GNU GPL v3.0 License](https://www.gnu.org/licenses/gpl-3.0.en.html).