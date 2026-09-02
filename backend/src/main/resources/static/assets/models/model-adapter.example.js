export async function mountMeetingRoomModel(context) {
    const { THREE, sceneGroup } = context;

    sceneGroup.clear();

    const placeholder = new THREE.Mesh(
        new THREE.BoxGeometry(2.8, 1.8, 2.2),
        new THREE.MeshStandardMaterial({
            color: 0xc7d4e5,
            transparent: true,
            opacity: 0.55,
            roughness: 0.3
        })
    );

    placeholder.position.set(0, 0.9, 0);
    sceneGroup.add(placeholder);

    return {
        update() {
            // 将模型联动逻辑写在这里。
        }
    };
}
