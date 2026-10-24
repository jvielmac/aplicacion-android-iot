## Configuración inicial

1. Clona el repositorio.

```
git clone https://github.com/jvielmac/aplicacion-android-iot.git
```

## Guía de contribución y uso de Git

### Flujo de trabajo usual en Git

Al iniciar trabajo en una nueva tarea (Ver [Uso de feature branches](#uso-de-feature-branches) para más información):

1. `git checkout main` Para cambiar a la rama `main`.
2. `git pull main` Para obtener el código más reciente del repositorio.
3. `git checkout -b <tarea>` Para crear una nueva rama.

Cada vez que quieras guardar el progreso de una tarea:

1. `git checkout <tarea>` Para cambiar a la tarea actual.
2. `git add .` Para agregar los archivos nuevos y modificados a staging (o sea, decirle a Git que queremos que incluya estos archivos en el commit).
3. `git commit -m "<descipción>"` Para crear un commit.

Al terminar el trabajo en una tarea:

1. `git checkout <tarea>`
2. `git push -u origin <tarea>` Para publicar los cambios.

### Uso de feature branches

En este repositorio se trabajará usando feature branches para organizar el código.

Nunca hacer push en `main` directamente, solo el código estable va en esta rama. Siempre crear una rama para la tarea en que vas a trabajar (por ejemplo, `feature/login-usuarios`) y hacer commits ahí, cuando la tarea este completada se hace un push para integrar el progreso a `main`.

Usar los siguientes formatos de nombre para las ramas:

- `feature/<nombre-caracteristica>` Para nueva funcionalidad.
- `bugfix/<error>` Para corregir errores.
- `docs/<descripción>` Para actualizar documentación.

### Buenas prácticas y consejos

- Hacer commits pequeños con frecuencia es mejor que hacer un commit que incorpora muchos cambios.

- Usa `git branch --list` para obtener la lista de ramas y saber cual es la rama actual.

- Usa `git status` para revisar los cambios antes de agregar los archivos a staging o hacer un commit.

- Usa `git log` o `git log --oneline` para revisar el historial de commits.

- Si hay cambios en `main` desde que iniciaste el trabajo en una tarea, ejecuta lo siguiente para crear un nuevo commit que incorpora los cambios a tu tarea actual:

```
git checkout main
git pull main
git checkout <tarea>
git merge main
```