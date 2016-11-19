//infinite, cyllindrical wire
//force/length~I_1*I_2/separation
//all units are SI
//uses force per length forMUla and mass per length instead of force and mass. works out the same. 

    public class Wire extends Thing
   {
      public double current;
      static final double MU_0=12*1e-7;
    
       public Wire(double[] rP, double[] vP, double massPerLength, double I)
      {
         super(rP,vP,massPerLength);
         current=I;
      }
   
       public double[] calculateForce(Wire other)
      {
         if(!this.equals(other))
         {
            double[] d=distance(other);
            double magnitude=MU_0*this.current*other.current/d[2];
            double f_x=magnitude*d[0]/d[2];
            double f_y=magnitude*d[1]/d[2];
            if(Math.abs(magnitude)<1e-1)//should take care of forces that overflow double. a greater max value would also fix it but is contextually irrelavent.
               return new double[]{f_x,f_y};
         }
         return new double[]{0,0};
      }
   }
   
   
   
