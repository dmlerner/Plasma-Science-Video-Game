   import java.util.ArrayList;

    public class Thing//generic object with position etc. responds to forces
   {
      public double[] r;//position
      public double[] v;//velocity
      public double[] a;//acceleration
      public double mass;
      static final double TIME_STEP=1/60.0;
   	
       public Thing()
      {
         for(int i=0;i<2;i++)
         {
            r[i]=0;
            v[i]=0;
         }
         mass=1;
      }
      
       public Thing(double[] rP, double[] vP, double m)
      {
         r=new double[2];
         v=new double[2];
         a=new double[2];
         for(int i=0;i<2;i++)
         {
            r[i]=rP[i];
            v[i]=vP[i];
            a[i]=0;
         }
         mass=m;
      }
   	
       public Thing(double[] rP)
      {
         r=new double[2];
         v=new double[2];
         a=new double[2];
         for(int i=0;i<2;i++)
         {
            r[i]=rP[i];
            v[i]=0;
            a[i]=0;
         }
         mass=1;
      }
      
       public void applyForce(double[] f)
      {
         for(int i=0;i<a.length;i++)
            a[i]+=f[i]/mass;			
      }
   
       public void step()
      {
         updateVelocity();
         updatePosition();
         for(int i=0;i<a.length;i++)
            a[i]=0;
      }
      
       public void updatePosition()
      {
         for(int i=0;i<2;i++)
            r[i]+=v[i]*TIME_STEP;
      }
      
       public void updateVelocity()
      {
         for(int i=0;i<2;i++)
            v[i]+=a[i]*TIME_STEP;
      }
      
       public double[] distance(Thing other)
      {
         double d[]=new double[2];
         double[] rOther=other.r;
         for(int i=0;i<2;i++)
            d[i]=this.r[i]-rOther[i];
         double magnitude=Math.sqrt(Math.pow(d[0],2)+Math.pow(d[1],2));
         return new double[]{d[0],d[1],magnitude};
      }
      
       public double[] calculateForce(Thing other)
      {
         if(other.equals(this))
            return new double[]{0,0};
         else
            return new double[]{0,0};
      }
   
       public boolean equals(Thing other)
      {
         double[] rOther=other.r;
         double[] vOther=other.v;
         for(int i=0;i<r.length;i++)
            if(r[i]!=rOther[i] || v[i]!=vOther[i])
               return false;
         return true;
      }
      	         	
       public boolean isInside(ArrayList<Thing> boundaryPoints)//boundary points must be added to the arraylist adjacently
      {//determines number of times that a vertical, upwards line from this crosses between two boundaryPoints
         int count=0;
         for(int i=0;i<boundaryPoints.size();i++)
         {
            Thing left=boundaryPoints.get(i);
            Thing right=boundaryPoints.get((i+1)%boundaryPoints.size());
            if((left.r[0]<this.r[0] && right.r[0]>this.r[0]) || (left.r[0]>this.r[0] && right.r[0]<this.r[0]))
               if(left.r[1]>this.r[1] || right.r[1]>this.r[1])
               {
                  double slope;
                  if(right.r[0]==left.r[0])//takes care of vertical lines with undefined slope
                     slope=1e-8;
                  else slope=(right.r[1]-left.r[1])/(right.r[0]-left.r[0]);
                  double yIntercept=right.r[1]-slope*right.r[0];
                  double y=slope*this.r[0]+yIntercept;
                  if(y>this.r[1])
                     count++;
               }
         }
         return count%2==1;//topology
      }
   }