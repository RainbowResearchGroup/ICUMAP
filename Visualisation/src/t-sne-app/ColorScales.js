class ColorScales {
   constructor() {
       this.layout = "default";
       this.updateScales();
   }

   setLayout(layout) {
       this.layout = layout;
       this.updateScales();
   }

   updateScales() {
       switch (this.layout) {
           case "default":
               this.dischargeSource =
                   [65, 166, -224];
               this.dischargeTarget =
                   [65, -247, 126];
               this.deathSource =
                   [65, 158, -224];
               this.deathTarget =
                   [65, 130, 167];
               break;
           case "default-2": // (like default, but with discharge/mortality pulled closer to LAB space)
               this.dischargeSource =
                   [65, 162, -220];
               this.dischargeTarget =
                   [65, -210, 70];
               this.deathSource =
                   [65, 152, -212];
               this.deathTarget =
                   [65, 132, 90];
               break;
           case "vomit":
               this.dischargeSource =
                   [65, -44, 100];
               this.dischargeTarget =
                   [65, 84, 68];
               this.deathSource =
                   [65, 20, -96];
               this.deathTarget =
                   [65, 84, -26];
               break;
           case "admission-LAB":
               this.dischargeSource =
                   [65, 12, -60];
               this.dischargeTarget =
                   [65, -46, 86];
               this.deathSource =
                   [65, 14, -62];
               this.deathTarget =
                   [65, 88, 8];
               break;
       }
   }
}
