    public class PlasmaWire extends Wire
   {
      private double naturalLength;//distance from center initially
      public static PlasmaWire central;
      double c1=5e-5;//tightness of repulsive c1/x term w/ central wire
      double c2=5e-4;//tightness of attractive c2x term w/ central wire
   	
       public PlasmaWire(double[] rP, double[] vP, double massPerLength, double I)
      {
         super(rP,vP,massPerLength,I);
         if(central!=null)
            naturalLength=Math.abs(distance(central)[2]);
         else naturalLength=0;
      }
      
       public double[] calculateForce(PlasmaWire other)//only call this if one of the two wires is central. prohibits any wire from touching central, has no force at separation of naturalLength
      {
         if(naturalLength==0)
            return new double[]{0,0};
         
         double[] d=distance(other);
         double magnitude=c1*(1/d[2]-1/naturalLength)-c2*(d[2]-naturalLength);
         double[] centralR=central.r;
        
         if(r[0]>centralR[0])//these eight lines determine which guide wire is interacting with central and appropriately directs the force
            return new double[]{magnitude,0};
         else if(r[0]<centralR[0])
            return new double[]{-1.0*magnitude,0};
         else if(r[1]>centralR[1])
            return new double[]{0,magnitude};
         else if(r[1]<centralR[1])
            return new double[]{0,-1.0*magnitude};
         return new double[]{0,0};
      }
   }