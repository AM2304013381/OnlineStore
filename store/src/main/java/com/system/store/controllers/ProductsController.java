package com.system.store.controllers;

import com.system.store.models.Product;
import com.system.store.models.ProductDto;
import com.system.store.services.ProductService;
import com.system.store.repositories.ProductsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository repo;

    @Autowired
    private ProductService productService;

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        return "products/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        return "products/register";
    }

        @GetMapping({"", "/"})
        public String showProductList(Model model) {
            List<Product> products = repo.findAll(Sort.by(Sort.Direction.ASC, "id"));
            model.addAttribute("products", products);
            return "products/index";
        }

        @GetMapping("/create")
        public String showCreatePage(Model model) {
            ProductDto productDto = new ProductDto();
            model.addAttribute("productDto", productDto);
            return "products/Createproduct";
        }

        @PostMapping("/create")
        public String createProduct(
                @Valid @ModelAttribute ProductDto productDto,
                BindingResult result
        ) {

            // Validate file upload
            MultipartFile image = productDto.getImageFile();
            if (image.isEmpty()) {
                result.addError(new FieldError("productDto", "imageFile", "The image file is required"));
            } else if (!image.getContentType().startsWith("image/")) {
                result.addError(new FieldError("productDto", "imageFile", "Only image files are allowed"));
            }

            if (result.hasErrors()) {
                return "products/Createproduct";
            }

            // Save image file
            String storageFileName = new Date().getTime() + "_" + image.getOriginalFilename();
            try {
                String uploadDir = "public/images";
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ex) {
                // Log the exception
                ex.printStackTrace(); // Replace with proper logging
                result.addError(new FieldError("productDto", "imageFile", "Failed to upload image file"));
                return "products/Createproduct";
            }

            // Map ProductDto to Product entity and save to the database
            Product product = new Product();
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());
            product.setImageFileName(storageFileName);
            product.setCreatedAt(new Date());

            repo.save(product);

            return "redirect:/products";
        }

        @GetMapping({"/search"})
        public String searchProducts(@RequestParam("query") String query, Model model) {
            List<Product> products = productService.searchByNameorCategory(query);
            model.addAttribute("products", products);
            return "products/index";
        }

        @GetMapping("/view")
        public String viewProduct(@RequestParam int id, Model model) {
            Product product = repo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid product id:" + id));
            model.addAttribute("product", product);
            return "products/ViewProducts";
        }

        @GetMapping("/edit")
        public String showEditPage(@RequestParam int id, Model model) {
            try {
                Product product = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product id:" + id));
                model.addAttribute("product", product);

                // Populate ProductDto from the product
                ProductDto productDto = new ProductDto();
                productDto.setName(product.getName());
                productDto.setBrand(product.getBrand());
                productDto.setCategory(product.getCategory());
                productDto.setPrice(product.getPrice());
                productDto.setDescription(product.getDescription());
                model.addAttribute("productDto", productDto);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                return "redirect:/products";  // redirect if an error occurs
            }
            return "products/EditProduct";  // return the edit page
        }

        @PostMapping("/edit")
        public String updateProduct(
                Model model,
                @RequestParam int id,
                @Valid @ModelAttribute ProductDto productDto,
                BindingResult result) {

            try {
                Product product = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product id:" + id));

                if (result.hasErrors()) {
                    return "products/EditProduct";  // return to the edit page if validation fails
                }

                if (!productDto.getImageFile().isEmpty()) {
                    // delete old image
                    String uploadDir = "public/images/";
                    Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                    try {
                        Files.delete(oldImagePath);
                    } catch (Exception ex) {
                        System.out.println("Exception:" + ex.getMessage());
                    }

                    // save new image file
                    MultipartFile image = productDto.getImageFile();
                    Date createdAt = new Date();
                    String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

                    try (InputStream inputStream = image.getInputStream()) {
                        Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
                    }

                    product.setImageFileName(storageFileName);  // set the new image file name
                }

                // Update other product details
                product.setName(productDto.getName());
                product.setBrand(productDto.getBrand());
                product.setCategory(productDto.getCategory());
                product.setPrice(productDto.getPrice());
                product.setDescription(productDto.getDescription());

                repo.save(product);  // save the updated product

            } catch (Exception ex) {
                System.out.println("Exception:" + ex.getMessage());
            }

            return "redirect:/products";  // redirect to products list after update
        }


        @GetMapping("/delete")
        public String deleteProduct(@RequestParam int id) {

            try {
                Product product = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product id:" + id));

                // Delete product image
                Path imagePath = Paths.get("public/images/" + product.getImageFileName());
                try {
                    Files.delete(imagePath);
                } catch (Exception ex) {
                    System.out.println("Exception:" + ex.getMessage());
                }

                // Delete the product
                repo.delete(product);

            } catch (Exception ex) {
                System.out.println("Exception:" + ex.getMessage());
            }

            return "redirect:/products";  // redirect to products list after deletion
        }
    }

