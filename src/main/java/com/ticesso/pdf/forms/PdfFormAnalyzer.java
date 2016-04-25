package com.ticesso.pdf.forms;

import com.innoq.stuff.FormField;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Analyzer for PDFs with forms.
 */
public class PdfFormAnalyzer implements Closeable {

    private final PDDocument document;
    private boolean closed;

    public PdfFormAnalyzer(File file) throws IOException {
        this(PDDocument.load(file));
    }

    public PdfFormAnalyzer(PDDocument document) {
        this.document = document;
    }

    public List<FormField> getFormFields() {
        final PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        return acroForm.getFields().stream()
                .map(this::toFormField)
                .collect(Collectors.toList());
    }

    public BufferedImage getImageOfPage(int page) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        return pdfRenderer.renderImageWithDPI(page, 150, ImageType.RGB);
    }

    public void fillPdf(Map<String, String> params, OutputStream out) throws IOException {
        final PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        for (PDField field : form.getFields()) {
            final String value = params.get(field.getFullyQualifiedName());
            if (field instanceof PDCheckBox) {
                PDCheckBox cb = (PDCheckBox) field;
                if ("on".equals(value)) {
                    cb.check();
                }
            } else if (value != null && !value.isEmpty()) {
                field.setValue(value);
            }
        }
        form.flatten();
        document.save(out);
    }

    private FormField toFormField(PDField pdField) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        final FormField result = new FormField(pdField.getFullyQualifiedName());
        withWidget(pdField, (w) -> {
            final PDPage page = getPage(w);
            final float pheight = page.getMediaBox().getHeight();
            final float pwidth = page.getMediaBox().getWidth();
            result.setPage(catalog.getPages().indexOf(page));
            result.setType(pdField.getFieldType());
            result.setLeft(w.getRectangle().getLowerLeftX() / pwidth);
            result.setTop((pheight - w.getRectangle().getUpperRightY()) / pheight);
            result.setHeight(w.getRectangle().getHeight() / pheight);
            result.setWidth(w.getRectangle().getWidth() / pwidth);
        });
        return result;
    }

    private void withWidget(PDField pdField, Consumer<PDAnnotationWidget> f) {
        final List<PDAnnotationWidget> widgets = pdField.getWidgets();
        if (widgets != null && !widgets.isEmpty()) {
            f.accept(widgets.get(0));
        }
    }

    private PDPage getPage(PDAnnotationWidget widget) {
        if (widget.getPage() != null) {
            return widget.getPage();
        } else {
            return document.getPage(0);
        }
    }

    public void close() throws IOException {
        document.close();
        closed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!closed) {
            System.err.println("PDF document not properly closed by application.");
            document.close();
        }
    }

}

