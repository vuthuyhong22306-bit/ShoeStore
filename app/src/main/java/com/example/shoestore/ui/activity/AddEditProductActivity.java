package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityAddEditProductBinding;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class AddEditProductActivity extends AppCompatActivity {
    private ActivityAddEditProductBinding binding;
    private ProductRepository productRepository;
    private int productId = -1;
    private Product currentProduct;
    private List<Category> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productRepository = new ProductRepository(getApplication());
        productId = getIntent().getIntExtra("PRODUCT_ID", -1);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(productId == -1 ? "Thêm sản phẩm" : "Sửa sản phẩm");
        }

        productRepository.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                categoryList = categories;
                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.getName());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerCategory.setAdapter(adapter);

                if (currentProduct != null) {
                    setCategorySpinnerSelection();
                }
            }
        });

        if (productId != -1) {
            productRepository.getProductById(productId).observe(this, product -> {
                if (product != null && currentProduct == null) {
                    currentProduct = product;
                    populateFields(product);
                }
            });
        }

        binding.btnSave.setOnClickListener(v -> saveProduct());
    }

    private void populateFields(Product product) {
        binding.etProductName.setText(product.getName());
        binding.etDescription.setText(product.getDescription());
        binding.etPrice.setText(String.valueOf(product.getPrice()));
        binding.etImageUrl.setText(product.getImage());
        binding.etStock.setText(String.valueOf(product.getStock()));
        binding.etSizes.setText(product.getSizes());
        binding.etColors.setText(product.getColors());
        setCategorySpinnerSelection();
    }

    private void setCategorySpinnerSelection() {
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getId() == currentProduct.getCategoryId()) {
                binding.spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void saveProduct() {
        String name = binding.etProductName.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();
        String image = binding.etImageUrl.getText().toString().trim();
        String stockStr = binding.etStock.getText().toString().trim();
        String sizes = binding.etSizes.getText().toString().trim();
        String colors = binding.etColors.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty() || categoryList.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);
        int categoryId = categoryList.get(binding.spinnerCategory.getSelectedItemPosition()).getId();

        if (productId == -1) {
            Product newProduct = new Product(name, desc, price, image, categoryId, stock, sizes, colors);
            productRepository.insertProduct(newProduct);
            Toast.makeText(this, "Đã thêm sản phẩm", Toast.LENGTH_SHORT).show();
        } else {
            currentProduct.setName(name);
            currentProduct.setDescription(desc);
            currentProduct.setPrice(price);
            currentProduct.setImage(image);
            currentProduct.setCategoryId(categoryId);
            currentProduct.setStock(stock);
            currentProduct.setSizes(sizes);
            currentProduct.setColors(colors);
            productRepository.updateProduct(currentProduct);
            Toast.makeText(this, "Đã cập nhật sản phẩm", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
