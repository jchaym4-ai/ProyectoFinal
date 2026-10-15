/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package TestPackages;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class PruebaPDFBox {

    public static void main(String[] args) throws Exception {

        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            documento.save("prueba-svb.pdf");
        }

        System.out.println("PDF generado correctamente.");
    }
}
