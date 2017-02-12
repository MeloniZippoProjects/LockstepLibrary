/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

/**
 *
 * @author Raff
 */
public class ActiveHandshakes
{
    public int value;

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
        final ActiveHandshakes other = (ActiveHandshakes) obj;
        return this.value == other.value;
    }
    
    public boolean equals(Integer integer)
    {
        return this.value == integer;
    }

    public int compareTo(Integer integer)
    {
        return this.value - integer;
    }
    
    public ActiveHandshakes(Integer val)
    {
        value = val;
    }
    
    public void setValue(Integer val)
    {
        value = val;
    }
}
