package com.ticesso.pdf.forms.service;

import com.innoq.stuff.PdfFormAnalyzer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint for the forms resource.
 */
@Path("/forms")
public class FormsRessource {

    @GET
    @Path("{id}/field-info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFieldInfo(@PathParam("id") String pdfId) {
        return withPdf(pdfId, (pdf) -> Response.ok(pdf.getFormFields()).build());
    }

    @GET
    @Path("{id}/image/{page}")
    @Produces("image/png")
    public Response getImage(
            @PathParam("id") String pdfId,
            @PathParam("page") int page) {
        return withPdf(pdfId, (pdf) -> Response.ok(pdf.getImageOfPage(page)).build());
    }

    @POST
    @Path("{id}/process")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/pdf")
    public Response process(
            @PathParam("id") String pdfId,
            MultivaluedMap<String, String> formParams) {
        return withPdf(pdfId, (pdf) -> {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.fillPdf(flatten(formParams), out);
            return Response.ok(out.toByteArray()).build();
        });
    }

    @FunctionalInterface
    private interface PdfFunction<T, R> {
        R apply(T t) throws IOException;
    }

    private Response withPdf(String pdfId, PdfFunction<PdfFormAnalyzer, Response> f) {
        final File pdfFile = new File("./content/", pdfId);
        if (!pdfFile.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            try (final PdfFormAnalyzer analyzer = new PdfFormAnalyzer(pdfFile)) {
                return f.apply(analyzer);
            } catch (IOException e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        }
    }

    private Map<String, String> flatten(MultivaluedMap<String, String> mmap) {
        final HashMap<String, String> fmap = new HashMap<>();
        for (String key : mmap.keySet()) {
            fmap.put(key, mmap.getFirst(key));
        }
        return fmap;
    }

}
