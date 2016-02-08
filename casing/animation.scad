use <casing.scad>


function anim_t() = [0,0,27 + $t*20];

color("LimeGreen")
box_w_holder();

translate(anim_t())
    rotate([0, 180, 0])
    color("DarkOrange")
    cover();
