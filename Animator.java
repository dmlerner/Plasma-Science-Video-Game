   import java.applet.*;
   import java.util.*;
   import java.awt.*;
   import java.awt.geom.*;
   import java.awt.event.*;
   import java.io.*;
   import javax.swing.*;

    public class Animator extends Applet implements Runnable, KeyListener//, MouseListener
   {
      ArrayList<Wire> magnets;//stationary magnets to control the plasma
      ArrayList<Thing> wall;//the points that specify the wall; must be added in order
      ArrayList<PlasmaWire> plasma;//the mobile point model of the plasma
      double[] translate;//r'=r+translate; puts the top left data point to which the program is scaled at the top left of the area of the screen not reserved for buffer
      double[] scale;//r'=r*scale; makes the points fill the used part of the screen
      int[] SCREEN_SIZE={400,950};//width,height
      int RADIUS=7;//controls the size of the magnet drawings, etc
      double[] BUFFER={.25,.25};//fraction of the screen left blank around the wall, x,y
      double playbackSpeed=10;//times real time speed; must be >=1; floor(playbackSpeed) is what's actually used
      Thread animation;
      boolean running=true;
      long lastMeasuredTime=System.currentTimeMillis();
      long points=0;
      Font font;

       public void init()
      {
         setSize(SCREEN_SIZE[0],SCREEN_SIZE[1]);
         addKeyListener(this);
         font=new Font("Monospaced",Font.BOLD,26);

         magnets=new ArrayList<Wire>();
         double[][] magnetLocations={{.490,.537},{.392,.15},{.392,-.289},{.473,-.467},{.763,-.437},{.86,-.223},{.769,.41},{.651,.482}};
         for(int i=0;i<magnetLocations.length;i++)
         {
            double[] pt=magnetLocations[i];
            magnets.add(new Wire(new double[]{pt[0],pt[1]},new double[]{0,0},1,0));
         }

         plasma=new ArrayList<PlasmaWire>();
         createPlasma();

         Wall w=new Wall();
         ArrayList<double[]> wallPoints=w.getPoints();
         wall=new ArrayList<Thing>();
         for(int i=0;i<wallPoints.size();i++)
            wall.add(new Thing(wallPoints.get(i),new double[]{0,0},1));
         scale(wall);

         animation=new Thread(this);
         animation.start();
      }

       public void restart()
      {
         destroy();
         animation=new Thread(this);
         createPlasma();
         for(int i=0;i<magnets.size();i++)//turn off the magnets
            magnets.get(i).current=0;
         running=true;
         animation.start();
         points=0;
         lastMeasuredTime=System.currentTimeMillis();
      }


       public void createPlasma()
      {
         plasma.clear();//remove the guide points from the plasma

         double r_0=.68;
         double z_0=0;
         double a=.12;
         double kappa=1.75;
         double delta=.2;
         double[] v=randomDuple(.005);

         PlasmaWire.central=new PlasmaWire(new double[]{r_0,z_0},v,1,1);//add the central wire first
         plasma.add(new PlasmaWire(new double[]{r_0+a/2,z_0},v,1,1));//add the other four from theta=0 by increasing theta
         plasma.add(new PlasmaWire(new double[]{r_0,z_0+kappa*a/2},v,1,1));
         plasma.add(new PlasmaWire(new double[]{r_0-delta*a,z_0},v,1,1));
         plasma.add(new PlasmaWire(new double[]{r_0,z_0-kappa*a/2},v,1,1));
      }

       public double[] randomDuple(double d)//returns a random tuple with each component from -d to +d
      {
         double[] duple=new double[2];
         for(int i=0;i<2;i++)
         {
            duple[i]=Math.random()*d;
            if(Math.random()>.5)
               duple[i]*=-1;
         }
         return duple;
      }

       public void destroy()
      {
         running=false;
         animation=null;
      }

       public void run()
      {
         while(running)//while the game is running, calculate the next playbackSpeed positions and display the last of them
         {
            for(int i=0;i<playbackSpeed;i++)
            {
               for(int j=0;j<magnets.size();j++)//apply the force from every magnet to every wire in the plasma
               {
                  Wire magnet=magnets.get(j);
                  for(int k=0;k<plasma.size();k++)
                  {
                     PlasmaWire p=plasma.get(k);
                     p.applyForce(p.calculateForce(magnet));
                  }
                  PlasmaWire.central.applyForce(PlasmaWire.central.calculateForce(magnet));
               }
               for(int j=0;j<plasma.size();j++)//apply all forces between each plasma wire and the central wire
               {
                  PlasmaWire p=plasma.get(j);
                  double[] force=p.calculateForce(PlasmaWire.central);
                  p.applyForce(force);
                  force[0]*=-1;//newton's third
                  force[1]*=-1;
                  PlasmaWire.central.applyForce(force);
               }


               plasma.get(0).a[1]=PlasmaWire.central.a[1];//these eight lines remove any torque on the plasma
               plasma.get(2).a[1]=PlasmaWire.central.a[1];
               plasma.get(0).v[1]=PlasmaWire.central.v[1];
               plasma.get(2).v[1]=PlasmaWire.central.v[1];

               plasma.get(1).a[0]=PlasmaWire.central.a[0];
               plasma.get(3).a[0]=PlasmaWire.central.a[0];
               plasma.get(1).v[0]=PlasmaWire.central.v[0];
               plasma.get(3).v[0]=PlasmaWire.central.v[0];

               for(int j=0;j<plasma.size();j++)
                  plasma.get(j).step();
               PlasmaWire.central.step();
            }
            try
            {
               animation.sleep((long)(1000*Wire.TIME_STEP));
               repaint();
            }
                catch(Exception e)
               {
                  System.out.println(e);
               }
         }
      }

       public void scale(ArrayList<Thing> boundaryPoints)//creates translate and scale vectors such that the points in boundaryPoints fit exactly within the area of the scren not reserved by buffer
      {
         double[] min=new double[2];//the min x and min y values
         double[] max=new double[2];//the max x and max y values
         double[] r0=boundaryPoints.get(0).r;
         for(int i=0;i<2;i++)
         {
            min[i]=r0[i];
            max[i]=r0[i];
         }
         for(Thing t:boundaryPoints)//set the min/max arrays to actually hold the min/max values by comparing with all the data points
         {
            double[] r=t.r;
            for(int i=0;i<r.length;i++)
            {
               if(r[i]<min[i])
                  min[i]=r[i];
               if(r[i]>max[i])
                  max[i]=r[i];
            }
         }
         double[] d=new double[max.length];//the range of x and y
         for(int i=0;i<d.length;i++)
            d[i]=max[i]-min[i];
         translate=new double[2];
         scale=new double[2];
         for(int i=0;i<2;i++)
         {
            translate[i]=-1.0*min[i];
            scale[i]=SCREEN_SIZE[i]/d[i];
         }
      }

       public void update(Graphics g)//reduces animation flicker by drawing frames ahead of time, offscreen
      {
         Image offScreenImage=createImage(getWidth(),getHeight());
         Graphics offScreenGraphics=offScreenImage.getGraphics();
         paint(offScreenGraphics);
         g.drawImage(offScreenImage,0,0,this);
      }

       public void paint(Graphics g)
      {
         Graphics2D g2=(Graphics2D)g;
         g2.setFont(font);
         g2.fill(new Rectangle2D.Double(0,0,getWidth(),getHeight()));//draw the background
         int[] magnetControls=new int[]{1,4,7,8,9,6,3,2};
         for(int i=0;i<magnets.size();i++)//draw the magnets
         {
            Wire m=magnets.get(i);
            double[] r=map(m.r);
            if(m.current!=0)
               g2.setColor(Color.GREEN);
            else
               g2.setColor(Color.GRAY);
            g2.fill(new Rectangle2D.Double(r[0]-2*RADIUS,r[1]-2*RADIUS,4*RADIUS,4*RADIUS));
            g2.setColor(Color.YELLOW);
            g2.drawString(magnetControls[i]+"",(int)r[0]-7,(int)r[1]+8);//label the magnets
         }

         double[][] contour=getContour(plasma,40);
         if(isInside(contour,wall))
            g2.setColor(Color.GREEN);
         else
         {
            g2.setColor(Color.RED);
            running=false;
            g2.drawString("Press spacebar",50,50);
            g2.drawString(" to play again.",50,70);
         }

         int n=50;//number of concentric contours to draw for shading
         double[] center=PlasmaWire.central.r;
         for(int i=n;i>0;i--)//work outside in; i is the contour number
         {
            Polygon contourI=new Polygon();
            double scaleFactor=(double)i/(double)n;
            for(int j=0;j<contour.length;j++)//j is the point number
            {
               double[] point=new double[2];
               for(int k=0;k<2;k++)//k is the component (x/y)
               {
                  point[k]=contour[j][k];
                  point[k]-=center[k];
                  point[k]*=scaleFactor;
                  point[k]+=center[k];
               }
               double[] pixel=map(point);
               contourI.addPoint((int)Math.round(pixel[0]),(int)(Math.round(pixel[1])));
            }
            int min1=25;//darkest color used (for outside contour) will be r/g/b=0/min/min
            int min2=35;//same as above for the red "you've lost" plasma
            int colorValueRunning=(int)(255-(255-min1)*scaleFactor);
            int colorValueNotRunning=(int)(255-(255-min2)*scaleFactor);
            if(isInside(contour,wall))
               g2.setColor(new Color(0,colorValueRunning,colorValueRunning));//cyan
            else
               g2.setColor(new Color(colorValueNotRunning,0,0));//red
            g2.fill(contourI);
         }

         g2.setColor(Color.LIGHT_GRAY);
         for(int i=0;i<wall.size();i++)//draw the wall with thin lines
         {
            double[] r1=map(wall.get(i).r);
            double[] r2=map(wall.get((i+1)%wall.size()).r);
            Line2D.Double line=new Line2D.Double(r1[0],r1[1],r2[0],r2[1]);
            g2.draw(line);
         }

         if(running)//increase the score
         {
            long elapsedTime=System.currentTimeMillis()-lastMeasuredTime;
            lastMeasuredTime=System.currentTimeMillis();
            points+=elapsedTime*playbackSpeed;
         }
         g2.setColor(new Color(0,0,255));//blue
         g2.drawString((int)(points*1e-2)+"",getWidth()-100,50);//draw the score
         g2.drawString((int)(playbackSpeed)+"x",getWidth()-100,70);//draw the speed
      }

       public boolean isInside(double[][] contour,ArrayList<Thing> bounds)//determines if contour is inside bounds
      {
         for(double[] d:contour)
         {
            Thing t=new Thing(d,new double[]{0,0},0);
            if(!t.isInside(bounds))
               return false;
         }
         return true;
      }

       public double[] map(double[] r)//returns the pixel associated with the data point r
      {
         double[] rPrime=new double[r.length];
         for(int i=0;i<r.length;i++)
            rPrime[i]=r[i];
         for(int i=0;i<2;i++)
         {
            rPrime[i]+=translate[i];
            rPrime[i]*=scale[i]*(1-BUFFER[i]);
            rPrime[i]+=BUFFER[i]*SCREEN_SIZE[i]/2;
         }
         return rPrime;
      }

       public double[] antiMap(double[] pixel)//returns the data point associated with pixel
      {
         double[] r=new double[2];
         for(int i=0;i<pixel.length;i++)
            r[i]=pixel[i];
         for(int i=0;i<2;i++)
         {
            r[i]-=BUFFER[i]*SCREEN_SIZE[i]/2;
            r[i]*=1.0/(scale[i]*(1-BUFFER[i]));
            r[i]-=translate[i];
         }
         return r;
      }

       public void keyReleased(KeyEvent e){}
       public void keyTyped(KeyEvent e){}
       public void keyPressed(KeyEvent e)//multiple controls for use with the button boards
      {
         int code=e.getKeyCode();
         if(code==KeyEvent.VK_SPACE || code==KeyEvent.VK_I)
            restart();
         else if(code==KeyEvent.VK_LEFT)
         {
            playbackSpeed-=5;
            if(playbackSpeed<1)
               playbackSpeed=1;
         }
         else if(code==KeyEvent.VK_RIGHT)
         {
            playbackSpeed+=5;
            if(playbackSpeed==6)
               playbackSpeed=5;
         }
         else if(code==KeyEvent.VK_NUMPAD1 || code==KeyEvent.VK_Z || code==KeyEvent.VK_B)
            toggleMagnet(0);
         else if(code==KeyEvent.VK_NUMPAD4 || code==KeyEvent.VK_A || code==KeyEvent.VK_G)
            toggleMagnet(1);
         else if(code==KeyEvent.VK_NUMPAD7 || code==KeyEvent.VK_Q || code==KeyEvent.VK_T)
            toggleMagnet(2);
         else if(code==KeyEvent.VK_NUMPAD8 || code==KeyEvent.VK_W || code==KeyEvent.VK_Y)
            toggleMagnet(3);
         else if(code==KeyEvent.VK_NUMPAD9 || code==KeyEvent.VK_E || code==KeyEvent.VK_U)
            toggleMagnet(4);
         else if(code==KeyEvent.VK_NUMPAD6 || code==KeyEvent.VK_D || code==KeyEvent.VK_J)
            toggleMagnet(5);
         else if(code==KeyEvent.VK_NUMPAD3 || code==KeyEvent.VK_C || code==KeyEvent.VK_M)
            toggleMagnet(6);
         else if(code==KeyEvent.VK_NUMPAD2 || code==KeyEvent.VK_X || code==KeyEvent.VK_N)
            toggleMagnet(7);
      }

       public void toggleMagnet(int i)
      {
         if(magnets.get(i).current!=0)
            magnets.get(i).current=0;
         else
            magnets.get(i).current=1e2;
      }

       public double[][] getContour(ArrayList<PlasmaWire> plasma,int n)//number them 0 to 3 from theta=0 to theta=3pi/2
      {
         double[][] points=new double[n][];
         double[] parameters=getParameters();

         double r_0=parameters[0];
         double z_0=parameters[1];
         double a=parameters[2];
         double k=parameters[3];
         double arcSinDelta=Math.asin(parameters[4]);

         for(int i=0;i<n;i++)//see Analytic Contour.pdf
         {
            double theta=Math.PI*2*i/n;
            points[i]=new double[2];
            points[i][0]=r_0+a*Math.cos(theta+Math.sin(theta)*arcSinDelta);
            points[i][1]=z_0+k*a*Math.sin(theta);
         }

         return points;
      }

       public double[] getParameters()
      {

         double r_0=PlasmaWire.central.r[0];
         double z_0=PlasmaWire.central.r[1];

         double a=PlasmaWire.central.distance(plasma.get(0))[2]*2;
         double k=(PlasmaWire.central.distance(plasma.get(1))[2]*2/a+PlasmaWire.central.distance(plasma.get(3))[2]*2/a)/2;
         double delta=PlasmaWire.central.distance(plasma.get(2))[2]/a;

         return new double[]{r_0,z_0,a,k,delta};
      }

   }
