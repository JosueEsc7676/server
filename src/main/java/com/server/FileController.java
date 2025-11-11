    package com.server;

    import org.springframework.core.io.FileSystemResource;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestParam;

    import java.io.File;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.util.Arrays;
    import java.util.List;
    import java.util.stream.Collectors;

    @Controller
    public class FileController {

        private static final String UPLOAD_DIR = "uploads";

        @GetMapping("/")
        public String home() {
            return "index";
        }

        @GetMapping("/android")
        public String android(Model model) {
            File folder = new File(UPLOAD_DIR + "/android");
            List<String> archivos = obtenerArchivos(folder);

            boolean tienePartes = archivos.stream().anyMatch(a -> a.startsWith("APP"));

            // Nombre visible para el usuario
            String nombreVisible = "MiApp Android";

            // Nombre interno para descarga (partes o archivo completo)
            String nombrePrograma = tienePartes ? "APP.part" : (archivos.isEmpty() ? "" : archivos.get(0));

            model.addAttribute("tipo", "android");
            model.addAttribute("nombreVisible", nombreVisible);
            model.addAttribute("nombrePrograma", nombrePrograma);
            model.addAttribute("disponible", !archivos.isEmpty());

            // Descripción fija o leída de archivo
            String descripcion = "Aplicación Android institucional - Permite gestionar procesos desde dispositivos móviles.";
            model.addAttribute("descripcion", descripcion);

            // Imagen (ruta relativa dentro de static o uploads)
            String imagen = "/img/android.png";
            model.addAttribute("imagen", imagen);

            return "android";
        }


        @GetMapping("/escritorio")
        public String escritorio(Model model) {
            File folder = new File(UPLOAD_DIR + "/escritorio");
            List<String> archivos = obtenerArchivos(folder);

            boolean tienePartes = archivos.stream().anyMatch(a -> a.startsWith("GDOC"));
            String nombrePrograma = tienePartes ? "GDOC.part" : (archivos.isEmpty() ? "" : archivos.get(0));

            model.addAttribute("tipo", "escritorio");
            model.addAttribute("nombrePrograma", nombrePrograma);
            model.addAttribute("disponible", !archivos.isEmpty());

            // Descripción fija o leída de archivo
            String descripcion = "Sistema de Gestión de Incapacidad de Docentes - Permite administrar incapacidades y generar reportes institucionales.";
            model.addAttribute("descripcion", descripcion);

            // Imagen (ruta relativa dentro de static o uploads)
            String imagen = "/img/programa.png";
            model.addAttribute("imagen", imagen);

            return "escritorio";
        }





        private List<String> obtenerArchivos(File folder) {
            if (!folder.exists() || !folder.isDirectory()) return List.of();
            return Arrays.stream(folder.listFiles())
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
        @GetMapping("/descargar")
        public ResponseEntity<FileSystemResource> descargar(
                @RequestParam String tipo,
                @RequestParam String nombre) throws IOException {

            File folder = new File(UPLOAD_DIR + "/" + tipo);

            // Buscar partes que empiecen con el nombre base
            File[] partes = folder.listFiles((dir, fname) -> fname.startsWith(nombre));

            if (partes != null && partes.length > 0) {
                Arrays.sort(partes);

                // Extensión según tipo
                String extension = tipo.equals("android") ? "" : ".exe";

                File archivoFinal = File.createTempFile(nombre, extension);
                try (FileOutputStream fos = new FileOutputStream(archivoFinal)) {
                    for (File parte : partes) {
                        java.nio.file.Files.copy(parte.toPath(), fos);
                    }
                }

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + extension + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new FileSystemResource(archivoFinal));
            }

            // Si no hay partes, descargar archivo normal
            File archivo = new File(folder, nombre);
            if (!archivo.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Extensión según tipo
            String extension = tipo.equals("android") ? ".apk" : ".exe";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + extension + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(archivo));
        }

    }
