package com.example.shoestore.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String description;
    private double price;
    private String image;
    private int categoryId;
    private int stock;
    private String sizes; // comma separated: "38,39,40,41,42"
    private String colors; // comma separated: "Đen,Trắng,Đỏ"

    public Product(String name, String description, double price, String image, int categoryId, int stock, String sizes, String colors) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
        this.categoryId = categoryId;
        this.stock = stock;
        this.sizes = sizes;
        this.colors = colors;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getSizes() { return sizes; }
    public void setSizes(String sizes) { this.sizes = sizes; }
    public String getColors() { return colors; }
    public void setColors(String colors) { this.colors = colors; }
}
