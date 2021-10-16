/*
 * Copyright (C) 2021 Oscar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pm.data;

import static java.math.RoundingMode.HALF_UP;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Oscar
 */
public abstract class Product implements Rateable<Product>, Comparable<Product> {


    public static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.1);
    private static int id = loadId();
    private String type;
    private String name;
    private BigDecimal price;
    private Rating rating;
    private Set<Review> reviews = new TreeSet<>();

    Product(String type, String name, BigDecimal price) {
        this(type, name, price, Rating.NOT_RATED);
    }

    Product(String type, String name, BigDecimal price, Rating rating) {
        autoIncrement();
        this.type = type;
        this.name = name;
        this.price = price;
        this.rating = rating;
    }

    Product(String type, String name, BigDecimal price, Rating rating, Set<Review> reviews) {
        this(type, name, price, rating);
        this.reviews = reviews;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDiscount() {
        return price.multiply(DISCOUNT_RATE).setScale(2, HALF_UP);
    }

    public Set<Review> getReviews() {
        return reviews;
    }
    public void setReview(Review review){
        this.reviews.add(review);
    }
    public String printReviews(){
        String review = "";
        for (Review r : reviews){
            review += r + "\n";
        }
        return review;
    }

    private static void autoIncrement(){
        id++;
    }

    private static int loadId(){
        int idMax = 0;

        try (Stream<Path> products= Files.list(Path.of(ResourceBundle.getBundle("pm.data.config").getString("data.folder")))){
            
            idMax = products.mapToInt(s -> Integer.valueOf(s.getFileName().toString().substring(7, 10))).max().orElse(0);

        } catch (IOException e) {
            System.out.println("Problem with the id's load");
        }
        return idMax;

    }
    @Override
    public Rating getRating() {
        return rating;
    }


    @Override
    public String toString() {
        return id + ", " + type + ", " + name + ", " + price + ", " + rating.getStars();
        
    }

    public LocalDate getBestBefore() {
        return LocalDate.now();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            final Product other = (Product) obj;
            return this.id == other.id;
        }
        return false;
    }

    @Override
    public int compareTo(Product o) {
        return this.id - o.id; 
    }
    
    

}
