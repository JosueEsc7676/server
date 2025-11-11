package com.server;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class FileController {

    private static final String UPLOAD_DIR = "static/uploads";

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/android")
    public String android(Model model) {
        String nombreVisible = "MiApp Android";
        String nombrePrograma = "base.apk"; // archivo en src/main/resources/static/uploads/android/

        model.addAttribute("tipo", "android");
        model.addAttribute("nombreVisible", nombreVisible);
        model.addAttribute("nombrePrograma", nombrePrograma);
        model.addAttribute("disponible", true);

        model.addAttribute("descripcion", "Aplicación Android institucional - Permite gestionar procesos desde dispositivos móviles.");
        model.addAttribute("imagen", "android.png");

        return "android";
    }

    @GetMapping("/escritorio")
    public String escritorio(Model model) {
        String nombreVisible = "GDOC Escritorio";
        String nombrePrograma = "GDOC.part"; // archivo en src/main/resources/static/uploads/escritorio/

        model.addAttribute("tipo", "escritorio");
        model.addAttribute("nombreVisible", nombreVisible);
        model.addAttribute("nombrePrograma", nombrePrograma);
        model.addAttribute("disponible", true);

        model.addAttribute("descripcion", "Sistema de Gestión de Incapacidad de Docentes - Permite administrar incapacidades y generar reportes institucionales.");
        model.addAttribute("imagen", "programa.png");

        return "escritorio";
    }

    @GetMapping("/descargar")
    public ResponseEntity<InputStreamResource> descargar(
            @RequestParam String tipo,
            @RequestParam String nombre) throws IOException {

        // Buscar todas las partes que comiencen con el nombre base
        List<ClassPathResource> partes = listarPartes(tipo, nombre);

        // Determinar extensión y limpiar nombre base
        String extension = tipo.equals("android") ? ".apk" : ".exe";
        String baseName = nombre.replace(".part", "").replace(".apk", "").replace(".exe", "");

        if (!partes.isEmpty()) {
            // Unir las partes en un archivo temporal
            File archivoFinal = File.createTempFile(baseName, extension);

            try (FileOutputStream fos = new FileOutputStream(archivoFinal)) {
                for (ClassPathResource parte : partes) {
                    try (InputStream in = parte.getInputStream()) {
                        in.transferTo(fos);
                    }
                }
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + extension + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(new FileInputStream(archivoFinal)));
        }

        // Si no hay partes, intentar descargar archivo único
        ClassPathResource resource = new ClassPathResource(UPLOAD_DIR + "/" + tipo + "/" + nombre);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + extension + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(resource.getInputStream()));
    }

    /**
     * Lista todas las partes de un archivo dentro del classpath.
     * Ejemplo: GDOC.part001, GDOC.part002...
     */
    private List<ClassPathResource> listarPartes(String tipo, String nombreBase) {
        // Definimos posibles sufijos de partes
        List<String> sufijos = Arrays.asList("001", "002", "003", "004", "005");

        return sufijos.stream()
                .map(suf -> new ClassPathResource(UPLOAD_DIR + "/" + tipo + "/" + nombreBase + suf))
                .filter(ClassPathResource::exists)
                .collect(Collectors.toList());
    }
}
