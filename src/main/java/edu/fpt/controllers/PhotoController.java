package edu.fpt.controllers;

import com.cloudinary.Cloudinary;
import com.cloudinary.Singleton;
import com.cloudinary.utils.ObjectUtils;
import edu.fpt.lib.PhotoUploadValidator;
import edu.fpt.models.Photo;
import edu.fpt.models.PhotoUpload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

@Controller
@RequestMapping("/")
public class PhotoController {

    String errorMessage = "";

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadPhoto(@ModelAttribute PhotoUpload photoUpload, BindingResult result, ModelMap model) throws IOException {
//        PhotoUploadValidator validator = new PhotoUploadValidator();
//        validator.validate(photoUpload, result);

        if(photoUpload.getTitle().isEmpty()){
            errorMessage = "Title is empty";
            System.out.println(errorMessage);
            return "redirect:/upload_form";
        }

        //validation
        if (photoUpload.getFile() == null || photoUpload.getFile().isEmpty()) {
            if (!photoUpload.validSignature()) {
                errorMessage = "Signature invalid";
                System.out.println(errorMessage);
                return "redirect:/upload_form";
            }
        }
        if(photoUpload.getFile().getSize()/(1048576)>2){
            errorMessage = "File upload over 2MB";
            System.out.println(errorMessage);
            return "redirect:/upload_form";
        }


        Map uploadResult = null;
        if (photoUpload.getFile() != null && !photoUpload.getFile().isEmpty()) {
            //get temp file in project
//            File tempFile = new File("src/main/resources/tempFile.tmp");
            //rename temp file to uploaded file
            File file = new File("src/main/resources/"+photoUpload.getFile().getOriginalFilename());
//            Files.move(tempFile.toPath(), file.toPath());
            //convert multipart file o file
            try (OutputStream os = new FileOutputStream(file)) {
                os.write(photoUpload.getFile().getBytes());
            }
            uploadResult = Singleton.getCloudinary().uploader().upload(file,
                    ObjectUtils.asMap("resource_type", "auto","use_filename","true","unique_filename","false"));
            // "use_filename","true" => keep original filename + and add id to the end
            // "unique_filename","false" => keep original filename and dont add anything
            file.delete();
            photoUpload.setPublicId((String) uploadResult.get("public_id"));
            Object version = uploadResult.get("version");
            if (version instanceof Integer) {
                photoUpload.setVersion(new Long((Integer) version));    
            } else {
                photoUpload.setVersion((Long) version);
            }
            photoUpload.setSignature((String) uploadResult.get("signature"));
            photoUpload.setFormat((String) uploadResult.get("format"));
            photoUpload.setResourceType((String) uploadResult.get("resource_type"));
        }

        if (result.hasErrors()){
            model.addAttribute("photoUpload", photoUpload);
            return "upload_form";
        } else {
            Photo photo = new Photo();
            photo.setTitle(photoUpload.getTitle());
            photo.setUpload(photoUpload);
            model.addAttribute("upload", uploadResult);
            model.addAttribute("photo", photo);
            return "upload";
        }
    }

    @RequestMapping(value = "/upload_form", method = RequestMethod.GET)
    public String uploadPhotoForm(ModelMap model) {
        model.addAttribute("photoUpload", new PhotoUpload());
        if(errorMessage.length()>0) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "upload_form";
    }

}
