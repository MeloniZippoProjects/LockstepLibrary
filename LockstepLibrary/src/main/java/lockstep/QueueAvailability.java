/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

/**
 *
 * @author enric
 */
public class QueueAvailability {
    boolean value;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueueAvailability other = (QueueAvailability) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }
    
    public boolean equals(Boolean bool)
    {
        return this.value == bool;
    }
    
    
    public QueueAvailability(Boolean val)
    {
        value = val;
    }
    
    public void setValue(Boolean val)
    {
        value = val;
    }
}
