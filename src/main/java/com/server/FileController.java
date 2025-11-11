package com.server;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Controller
public class FileController {

    private static final String UPLOAD_DIR = "static/uploads";

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/android")
    public String android(Model model) throws IOException {
        List<String> archivos = obtenerArchivos(UPLOAD_DIR + "/android");

        boolean tienePartes = archivos.stream().anyMatch(a -> a.startsWith("APP"));
        String nombreVisible = "MiApp Android";
        String nombrePrograma = tienePartes ? "APP.part" : (archivos.isEmpty() ? "" : archivos.get(0));

        model.addAttribute("tipo", "android");
        model.addAttribute("nombreVisible", nombreVisible);
        model.addAttribute("nombrePrograma", nombrePrograma);
        model.addAttribute("disponible", !archivos.isEmpty());

        model.addAttribute("descripcion", "Aplicación Android institucional - Permite gestionar procesos desde dispositivos móviles.");
        model.addAttribute("imagen", "/img/android.png");

        return "android";
    }

    @GetMapping("/escritorio")
    public String escritorio(Model model) throws IOException {
        List<String> archivos = obtenerArchivos(UPLOAD_DIR + "/escritorio");

        boolean tienePartes = archivos.stream().anyMatch(a -> a.startsWith("GDOC"));
        String nombrePrograma = tienePartes ? "GDOC.part" : (archivos.isEmpty() ? "" : archivos.get(0));

        model.addAttribute("tipo", "escritorio");
        model.addAttribute("nombrePrograma", nombrePrograma);
        model.addAttribute("disponible", !archivos.isEmpty());

        model.addAttribute("descripcion", "Sistema de Gestión de Incapacidad de Docentes - Permite administrar incapacidades y generar reportes institucionales.");
        model.addAttribute("imagen", "/img/programa.png");

        return "escritorio";
    }

    /**
     * Método para listar archivos tanto en local como dentro de un JAR.
     */
    private List<String> obtenerArchivos(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        URL url = resource.getURL();

        if ("jar".equals(url.getProtocol())) {
            String jarPath = url.getPath().split("!")[0].replace("file:", "");
            try (JarFile jarFile = new JarFile(jarPath)) {
                return jarFile.stream()
                        .map(JarEntry::getName)
                        .filter(name -> name.startsWith("BOOT-INF/classes/" + path))
                        .map(name -> name.substring(("BOOT-INF/classes/" + path).length() + 1))
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toList());
            }
        } else {
            File folder = resource.getFile();
            if (!folder.exists() || !folder.isDirectory()) return List.of();
            return Arrays.stream(folder.listFiles())
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
    }

    @GetMapping("/descargar")
    public ResponseEntity<FileSystemResource> descargar(
            @RequestParam String tipo,
            @RequestParam String nombre) throws IOException {

        // Aquí puedes seguir usando ClassPathResource si necesitas unir partes
        File folder = new ClassPathResource(UPLOAD_DIR + "/" + tipo).getFile();
        File[] partes = folder.listFiles((dir, fname) -> fname.startsWith(nombre));

        if (partes != null && partes.length > 0) {
            Arrays.sort(partes);

            String extension = tipo.equals("android") ? ".apk" : ".exe";
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

        File archivo = new File(folder, nombre);
        if (!archivo.exists()) {
            return ResponseEntity.notFound().build();
        }

        String extension = tipo.equals("android") ? ".apk" : ".exe";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getName() + extension + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(archivo));
    }
}
