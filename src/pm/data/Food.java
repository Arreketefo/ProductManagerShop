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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 *
 * @author Oscar
 */
public final class Food extends Product{

    private LocalDate bestBefore;
    
    Food(String type, String name, BigDecimal price, Rating rating, LocalDate bestBefore){
        super(type, name, price, rating);
        this.bestBefore = bestBefore;

    }
    Food(String type, String name, BigDecimal price, Rating rating, LocalDate bestBefore, Set<Review> reviews){
        super(type, name, price, rating, reviews);
        this.bestBefore = bestBefore;
    }
    

    @Override
    public LocalDate getBestBefore(){
        return this.bestBefore;
    }
    
    @Override
    public BigDecimal getDiscount() {
        return (bestBefore.equals(LocalDate.now())) ? super.getDiscount() : BigDecimal.ZERO;
    }
    @Override
    public String toString() {
        return  super.toString() + ", " + bestBefore + "\n" + printReviews();
    }

    @Override
    public Product applyRating(Rating newRating) {
        return new Food(getType(), getName(), getPrice(), newRating, bestBefore);
        }
    
    
}
